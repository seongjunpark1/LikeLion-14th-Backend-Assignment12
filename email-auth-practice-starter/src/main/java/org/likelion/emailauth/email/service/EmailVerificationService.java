package org.likelion.emailauth.email.service;

import lombok.RequiredArgsConstructor;
import org.likelion.emailauth.email.entity.EmailVerification;
import org.likelion.emailauth.email.repository.EmailVerificationRepository;
import org.likelion.emailauth.global.exception.EmailVerificationException;
import org.springframework.cglib.core.Local;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    // TODO: 인증번호가 몇 분 동안 유효한지 나타내는 상수입니다.
    private static final long VERIFICATION_CODE_EXPIRATION_MINUTES = 5;
    // TODO: 6자리 숫자 인증번호를 만들기 위한 최소값입니다.
//    private static final int MIN_VERIFICATION_CODE = 100000;
//    // TODO: 6자리 숫자 인증번호를 만들기 위한 최대값입니다.
//    private static final int MAX_VERIFICATION_CODE = 999999;
    //예시
    private static final int VERIFICATION_CODE_LENGTH = 6;
    private static final String VERIFICATION_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private final EmailVerificationRepository emailVerificationRepository;
    private final JavaMailSender javaMailSender;

    @Transactional
    public void sendVerificationCode(String email) {
        String verificationCode = generateVerificationCode();
        EmailVerification emailVerification = saveOrUpdateEmailVerification(email, verificationCode);
        sendEmail(emailVerification.getEmail(), verificationCode);
    }

    @Transactional
    public void verifyCode(String email, String code) {
        // TODO: 이메일에 해당하는 인증정보를 DB에서 조회합니다.
        // TODO: 인증정보가 없다면 인증 요청 정보를 찾을 수 없다는 예외를 발생시킵니다.

        EmailVerification emailVerification = emailVerificationRepository.findByEmail(email)
                .orElseThrow(() -> new EmailVerificationException("인증 요청 정보를 찾을 수 없습니다", HttpStatus.NOT_FOUND));
        // TODO: 인증번호의 만료 시간이 현재 시간보다 이전인지 확인합니다.

        if(emailVerification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new EmailVerificationException("인증번호가 만료되었습니다.", HttpStatus.BAD_REQUEST);
        }
        // TODO: 사용자가 입력한 인증번호와 DB에 저장된 인증번호를 비교합니다.
        // TODO: 인증에 성공하면 사용한 인증정보를 삭제해 같은 인증번호를 다시 사용할 수 없게 합니다.

        if (!emailVerification.getVerificationCode().equals(code)) {
            throw new EmailVerificationException("인증번호가 올바르지 않습니다.", HttpStatus.BAD_REQUEST);
        }

        emailVerificationRepository.delete(emailVerification);
    }

    private String generateVerificationCode() {
        // TODO: 사용자의 이메일로 전송할 랜덤 인증문자를 생성합니다.
        List<Character> chars = new ArrayList<>();
        for (char c : VERIFICATION_CODE_CHARS.toCharArray()) {
            chars.add(c);
        }
        Collections.shuffle(chars, new SecureRandom());

        // TODO: 지정된 알파벳 범위 안에서 6자리 인증문자가 중복 없이 만들어지도록 구현합니다.
        StringBuilder sb = new StringBuilder(VERIFICATION_CODE_LENGTH);
        for (int i = 0; i < VERIFICATION_CODE_LENGTH; i++) {
            sb.append(chars.get(i));
        }
        return sb.toString();
    }

    private EmailVerification saveOrUpdateEmailVerification(String email, String verificationCode) {
        // TODO: 현재 시간에 인증번호 유효시간을 더해 만료 시간을 계산합니다.
        LocalDateTime expireAt = LocalDateTime.now().plusMinutes(VERIFICATION_CODE_EXPIRATION_MINUTES);
        // TODO: 해당 이메일로 저장된 기존 인증정보가 있는지 Repository로 조회합니다.
        // 없다면 새로운 인증정보 생성하빈다
        EmailVerification emailVerification = emailVerificationRepository.findByEmail(email)
                .orElseGet(() -> EmailVerification.create(email, verificationCode, expireAt));

        // TODO: 기존 데이터가 있다면 새로운 인증번호와 만료 시간으로 갱신합니다.
        emailVerification.updateVerificationCode(verificationCode, expireAt);
        // TODO: 기존 데이터가 없다면 Entity의 생성 메서드로 새로운 인증정보를 만듭니다.
        // TODO: 생성 또는 갱신된 인증정보를 DB에 저장하고 반환합니다.
        return emailVerificationRepository.save(emailVerification);
    }

    private void sendEmail(String email, String verificationCode) {
        // TODO: SimpleMailMessage 객체를 생성합니다.
        SimpleMailMessage message = new SimpleMailMessage();
        // TODO: 인증번호를 받을 수신 이메일 주소를 설정합니다.
        message.setTo(email);
        // TODO: 메일 제목을 설정합니다.
        message.setSubject("[멋쟁이사자처럼] 이메일 인증문자");
        // TODO: 인증번호와 유효시간이 포함된 메일 본문을 작성합니다.
        message.setText("인증문자는 " + verificationCode + " 입니다.\n" +
                "인증문자는 " + VERIFICATION_CODE_EXPIRATION_MINUTES + "분 동안 유효합니다");
        // TODO: JavaMailSender를 사용해 Gmail SMTP로 실제 메일을 전송합니다.

        javaMailSender.send(message);
    }




}