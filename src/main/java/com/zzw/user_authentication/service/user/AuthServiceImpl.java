package com.zzw.user_authentication.service.user;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.zzw.user_authentication.config.JwtConfigPropertis;
import com.zzw.user_authentication.domain.entity.SysUser;
import com.zzw.user_authentication.domain.repository.SysUserRepository;
import com.zzw.user_authentication.service.jwt.JwtTokenService;

@Service
public class AuthServiceImpl implements AuthService {

	private AuthenticationManager authenticationManager;
	private UserDetailsService userDetailsService;
	private JwtTokenService jwtTokenService;
	private SysUserRepository userRepository;
	private JwtConfigPropertis jwtConfigPropertis;

	public AuthServiceImpl(AuthenticationManager authenticationManager, UserDetailsService userDetailsService,
			JwtTokenService jwtTokenUtil, SysUserRepository userRepository, JwtConfigPropertis jwtConfigPropertis) {
		this.authenticationManager = authenticationManager;
		this.userDetailsService = userDetailsService;
		this.jwtTokenService = jwtTokenUtil;
		this.userRepository = userRepository;
		this.jwtConfigPropertis = jwtConfigPropertis;
	}

	@Override
	public SysUser register(SysUser userToAdd) {
		final String username = userToAdd.getUsername();
		if (userRepository.findByUsername(username) != null) {
			return null;
		}
		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
		final String rawPassword = userToAdd.getPassword();
		userToAdd.setPassword(encoder.encode(rawPassword));
		return userRepository.save(userToAdd);
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
}