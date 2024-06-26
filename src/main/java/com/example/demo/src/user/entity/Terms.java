package com.example.demo.src.user.entity;

import com.example.demo.common.entity.BaseEntity;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDate;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Builder
@Getter
@Entity
public class Terms extends BaseEntity {
    @Id
    @Column(nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private boolean dataTermsConsent;
    @Column(nullable = false)
    private boolean usageTermsConsent;
    @Column(nullable = false)
    private boolean locationTermsConsent;
    @Column(nullable = false)
    private LocalDate consentDate;
    @Column(nullable = false)
    private TermsConsentState consentState;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, updatable = false)
    private User user;

    public enum TermsConsentState {
        VALID, EXPIRED;
    }

    public void updateConsent(
            Boolean dataTermsConsent,
            Boolean usageTermsConsent,
            Boolean locationTermsConsent
    ) {
        if (dataTermsConsent && usageTermsConsent && locationTermsConsent) {
            this.consentState = TermsConsentState.VALID;
            this.consentDate = LocalDate.now();
        }
    }

    public void expire() {
        this.consentState = TermsConsentState.EXPIRED;
    }
}
