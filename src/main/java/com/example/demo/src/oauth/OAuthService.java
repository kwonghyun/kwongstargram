package com.example.demo.src.oauth;

import com.example.demo.common.Constant;
import com.example.demo.common.exceptions.BaseException;
import com.example.demo.src.oauth.entity.OAuth;
import com.example.demo.src.oauth.model.KakaoOAuthToken;
import com.example.demo.src.oauth.model.KakaoUser;
import com.example.demo.src.user.entity.User;
import com.example.demo.src.user.model.GetSocialOAuthRes;
import com.example.demo.src.user.model.PostUserReq;
import com.example.demo.src.user.UserService;
import com.example.demo.utils.JwtService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

import static com.example.demo.common.entity.BaseEntity.State.ACTIVE;
import static com.example.demo.common.response.BaseResponseStatus.*;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OAuthService {
    private final OAuthRepository oAuthRepository;
    private final KakaoAuthenticationService kakaoAuthenticationService;
    private final HttpServletResponse response;
    private final JwtService jwtService;
    private final UserService userService;


    public void accessRequest(Constant.SocialLoginType socialLoginType) throws IOException {
        String redirectURL;
        switch (socialLoginType){ //각 소셜 로그인을 요청하면 소셜로그인 페이지로 리다이렉트 해주는 프로세스이다.
            case KAKAO:{
                redirectURL= kakaoAuthenticationService.getOauthRedirectURL();
            } break;
            default:{
                throw new BaseException(INVALID_OAUTH_TYPE);
            }
        }

        response.sendRedirect(redirectURL);
    }

    public void redirectSignUpPage(Constant.SocialLoginType socialLoginType, String code) throws IOException {
        String signUpPageUrl = "/sign-up?loginType="+ socialLoginType.name().toLowerCase() + "&code=";
        response.sendRedirect(signUpPageUrl + code);
    }


    @Transactional
    public GetSocialOAuthRes oAuthLoginOrRedirectToSignUp(Constant.SocialLoginType socialLoginType, String code) throws IOException {

        switch (socialLoginType) {
            case KAKAO: {
                //구글로 일회성 코드를 보내 액세스 토큰이 담긴 응답객체를 받아옴
                ResponseEntity<String> accessTokenResponse = kakaoAuthenticationService.requestAccessToken(code);
                log.info("토큰 받아오기");
                //응답 객체가 JSON형식으로 되어 있으므로, 이를 deserialization해서 자바 객체에 담을 것이다.
                KakaoOAuthToken oAuthToken = kakaoAuthenticationService.getAccessToken(accessTokenResponse);

                //우리 서버의 db와 대조하여 해당 user가 존재하는 지 확인한다.
                if(checkOAuthByExternalId(oAuthToken.extractEmail())) { // user가 DB에 있다면, 로그인 진행
                    //액세스 토큰을 다시 구글로 보내 구글에 저장된 사용자 정보가 담긴 응답 객체를 받아온다.
                    ResponseEntity<String> userInfoResponse = kakaoAuthenticationService.requestUserInfo(oAuthToken.getAccess_token());
                    //다시 JSON 형식의 응답 객체를 자바 객체로 역직렬화한다.
                    KakaoUser kakaoUser = kakaoAuthenticationService.getUserInfo(userInfoResponse);
                    // 유저 정보 조회
                    User user = getOAuthUserByExternalId(kakaoUser.getKakaoAccount().getEmail());
                    user.updateLastLoginAt();

                    //서버에 user가 존재하면 앞으로 회원 인가 처리를 위한 jwtToken을 발급한다.
                    String accessToken = jwtService.createJwt(user.getId(), user.getName(), user.getAuthority());

                    //액세스 토큰과 jwtToken, 이외 정보들이 담긴 자바 객체를 다시 전송한다.
                    GetSocialOAuthRes getSocialOAuthRes = new GetSocialOAuthRes(accessToken, user.getId(), oAuthToken.getAccess_token(), oAuthToken.getToken_type());
                    return getSocialOAuthRes;
                }else { // user가 DB에 없다면, token을 쿼리스트링에 담아 회원가입 페이지로 리다이렉트
                    redirectSignUpPage(socialLoginType, oAuthToken.getAccess_token());
                    return new GetSocialOAuthRes();
                }
            }
            default: {
                throw new BaseException(INVALID_OAUTH_TYPE);
            }

        }
    }

    @Transactional
    public OAuth createOAuth(Constant.SocialLoginType socialLoginType, String code, User user) throws JsonProcessingException {

        switch (socialLoginType) {
            case KAKAO: {
                log.info("카카오에서 유저정보 파싱");
                ResponseEntity<String> userInfoResponse = kakaoAuthenticationService.requestUserInfo(code);
                //다시 JSON 형식의 응답 객체를 자바 객체로 역직렬화한다.
                log.info("OAuth정보 저장");
                KakaoUser kakaoUser = kakaoAuthenticationService.getUserInfo(userInfoResponse);

                return oAuthRepository.save(kakaoUser.toEntity(user));
            }
            default: {
                throw new BaseException(INVALID_OAUTH_TYPE);
            }
        }
    }

    @Transactional
    public void createOAuthUser(PostUserReq postUserReq, Constant.SocialLoginType socialLoginType, String oAuthAccessToken) throws JsonProcessingException {
        User newUser = userService.createUser(postUserReq);
        createOAuth(socialLoginType, oAuthAccessToken, newUser);
    }

    public boolean checkOAuthByExternalId(String externalId) {
        Optional<OAuth> result = oAuthRepository.findByExternalIdAndState(externalId, ACTIVE);
        if (result.isPresent() && result.get().getUser().getState() == ACTIVE) return true;
        return false;
    }

    public User getOAuthUserByExternalId(String externalId) {
        OAuth oAuth = oAuthRepository.findByExternalIdAndState(externalId, ACTIVE)
                .orElseThrow(() -> new BaseException(NOT_FIND_USER));
        return oAuth.getUser();
    }

    @Transactional
    public void deleteByUserId(Long userId) {
        OAuth oAuth = oAuthRepository.findByUserIdAndState(userId, ACTIVE)
                .orElseThrow(() -> new BaseException(NOT_FIND_OAUTH_USER));
        oAuth.delete();
        userService.deleteUser(oAuth.getUser().getId());
    }

}
