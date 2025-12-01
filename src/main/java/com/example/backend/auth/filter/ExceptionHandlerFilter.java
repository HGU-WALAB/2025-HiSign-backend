package com.example.backend.auth.filter;

import com.example.backend.auth.exception.DoNotLoginException;
import com.example.backend.auth.exception.WrongTokenException;
import com.example.backend.base.response.ExceptionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ExceptionHandlerFilter extends OncePerRequestFilter {
  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      FilterChain filterChain)
      throws ServletException, IOException {

    try {
      filterChain.doFilter(request, response);
    } catch (DoNotLoginException e) {
      // 토큰의 유효기간 만료
      setErrorResponse(response, e.getMessage(), HttpStatus.UNAUTHORIZED);
    } catch (WrongTokenException e) {
      // 유효하지 않은 토큰
      setErrorResponse(response, e.getMessage(), HttpStatus.UNAUTHORIZED);
    }
  }

  private void setErrorResponse(
      HttpServletResponse response, String message, HttpStatus httpStatus) {
    ObjectMapper objectMapper = new ObjectMapper();
    response.setStatus(httpStatus.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    ExceptionResponse exceptionResponse =
        ExceptionResponse.builder().error(httpStatus.getReasonPhrase()).message(message).build();
    try {
      response.getWriter().write(objectMapper.writeValueAsString(exceptionResponse));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
