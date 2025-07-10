package ru.college.carmarketplace.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {


  private String name;
  private String email;
  private String password;
  private String confirmCode;
  private String type;
  //private Role role;
}
