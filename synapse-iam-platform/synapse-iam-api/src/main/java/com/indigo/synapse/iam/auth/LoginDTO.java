package com.indigo.synapse.iam.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author 史偕成
 * @date 2026/06/25 13:17
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginDTO implements Serializable {

    private String username;

    private String password;
}
