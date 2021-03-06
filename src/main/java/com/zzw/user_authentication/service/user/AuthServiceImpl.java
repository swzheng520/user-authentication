package com.zzw.user_authentication.service.user;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Base64;

import javax.crypto.Cipher;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.zzw.user_authentication.config.AuthConfigPropertis;
import com.zzw.user_authentication.config.JwtConfigPropertis;
import com.zzw.user_authentication.domain.entity.SysUser;
import com.zzw.user_authentication.domain.repository.UserRepository;
import com.zzw.user_authentication.service.jwt.JwtTokenService;
import com.zzw.user_authentication.util.LoginPasswordUtil;
import com.zzw.user_authentication.util.RsaCypherUtil;

@Service
public class AuthServiceImpl implements AuthService {

	private AuthenticationManager authenticationManager;
	private UserDetailsService userDetailsService;
	private JwtTokenService jwtTokenService;
	private UserRepository userRepository;
	private JwtConfigPropertis jwtConfigPropertis;
	private AuthConfigPropertis authConfigPropertis;

	public AuthServiceImpl(AuthenticationManager authenticationManager, UserDetailsService userDetailsService,
			JwtTokenService jwtTokenUtil, UserRepository userRepository, JwtConfigPropertis jwtConfigPropertis,
			AuthConfigPropertis authConfigPropertis) {
		this.authenticationManager = authenticationManager;
		this.userDetailsService = userDetailsService;
		this.jwtTokenService = jwtTokenUtil;
		this.userRepository = userRepository;
		this.jwtConfigPropertis = jwtConfigPropertis;
		this.authConfigPropertis = authConfigPropertis;
	}

	@Override
	public String register(SysUser userToAdd) throws Exception {
		final String username = userToAdd.getUsername();
		if (userRepository.findByUsername(username) != null) {
			throw new Exception("用户已存在");
		}
		String rawPassword = userToAdd.getPassword();
		if (authConfigPropertis.getIsEncryptPassword()) {
			rawPassword = LoginPasswordUtil.decryptedPassword(username, userToAdd.getPassword());
		}
		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
		userToAdd.setPassword(encoder.encode(rawPassword));
		userToAdd = userRepository.save(userToAdd);
		if (!Strings.isNullOrEmpty(userToAdd.getId())) {
			return "success";
		}
		return "fail";
	}

	@Override
	public String login(String username, String password) {
		UsernamePasswordAuthenticationToken upToken = new UsernamePasswordAuthenticationToken(username, password);
		// Perform the security
		final Authentication authentication = authenticationManager.authenticate(upToken);
		SecurityContextHolder.getContext().setAuthentication(authentication);
		// Reload password post-security so we can generate token
		final UserDetails userDetails = userDetailsService.loadUserByUsername(username);
		final String token = jwtTokenService.generateToken(userDetails);
		return "Bearer " + token;
	}

	@Override
	public String refresh(String oldToken) {
		final String token = oldToken.substring(jwtConfigPropertis.getTokenHead().length());
		// String username = jwtTokenService.getUsernameFromToken(token);
		// SysUser user = (SysUser)
		// userDetailsService.loadUserByUsername(username);
		if (jwtTokenService.canTokenBeRefreshed(token)) {
			return jwtTokenService.refreshToken(token);
		}
		return null;
	}

	@Override
	public String publicKey(String userName) {
		try {
			KeyPair keyPair = RsaCypherUtil.buildKeyPair();
			LoginPasswordUtil.getUserKeyPair().put(userName, keyPair);
			PublicKey publicKey = keyPair.getPublic();
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.ENCRYPT_MODE, publicKey);
			byte[] str = cipher.doFinal(userName.getBytes());
			return Base64.getEncoder().encodeToString(str);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}
}