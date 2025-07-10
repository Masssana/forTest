package ru.college.carmarketplace.service.impl;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.college.carmarketplace.exception.CustomException;
import ru.college.carmarketplace.model.RegisterRequest;
import ru.college.carmarketplace.model.entities.VerificationCode;
import ru.college.carmarketplace.repo.UserRepository;
import ru.college.carmarketplace.repo.VerificationCodeRepository;
import ru.college.carmarketplace.service.AuthActionProcess;

@RequiredArgsConstructor
@Service
public class ConfirmEmail implements AuthActionProcess {

    private final VerificationCodeRepository verificationCodeRepository;
    private final UserRepository repository;

    @Override
    public void process(RegisterRequest registerRequest) throws CustomException {
        VerificationCode verificationCode = verificationCodeRepository.findByEmail(registerRequest.getEmail())
                .orElseThrow(() -> new CustomException("Код подтверждения не найден"));

        if (!verificationCode.getConfirmCode().equals(registerRequest.getConfirmCode())) {
            throw new CustomException("Код не сходится");
        }

        // Находим пользователя и помечаем его email как подтвержденный
        var user = repository.findByEmail(registerRequest.getEmail());
        if (user == null) {
            throw new CustomException("Пользователь не найден");
        }

        user.setConfirmed(true);
        repository.save(user);

        // Удаляем код подтверждения
        verificationCodeRepository.delete(verificationCode);
    }
}
