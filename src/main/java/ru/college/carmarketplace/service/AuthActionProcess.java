package ru.college.carmarketplace.service;

import ru.college.carmarketplace.model.RegisterRequest;

public interface AuthActionProcess {

    void process(RegisterRequest registerRequest);
}
