package com.example.backend.auth.service;

import com.example.backend.auth.dto.AuthDto;
import com.example.backend.auth.exception.FailedHisnetLoginException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

@Service
public class HisnetLoginService {

  @Value("${hisnet.access-key}")
  private String accessKey;

  public AuthDto callHisnetLoginApi(AuthDto dto) {
    Map<String, Object> requestBody = new HashMap<>();

    requestBody.put("token", dto.getHisnetToken());
    requestBody.put("accessKey", accessKey);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

    RestTemplate restTemplate = new RestTemplate();
    String url = "https://walab.info:8443/HisnetLogin/api/hisnet/login/validate";
    UriComponents uri = UriComponentsBuilder.fromHttpUrl(url).build();

    try {
      ParameterizedTypeReference<Map<String, Object>> typeRef =
          new ParameterizedTypeReference<Map<String, Object>>(){};
      ResponseEntity<Map<String, Object>> resultMap =
          restTemplate.exchange(uri.toString(), HttpMethod.POST, entity, typeRef);
      Map<String, Object> result = resultMap.getBody();
      assert result != null;
      String uniqueId = (String) result.get("uniqueId");
      String name = (String) result.get("name");
      String email = (String) result.get("email");
      String department = (String) result.get("department");
      String major1 = (String) result.get("major1");
      String major2 = (String) result.get("major2");
      Integer grade = result.get("grade") != null ? Integer.parseInt(result.get("grade").toString()) : null;
      int semester = Integer.parseInt(result.get("semester").toString());
      int level = (grade == null && major1 == null && major2 == null && semester == 0) ? 1 : 0;

      return  AuthDto.builder()
              .uniqueId(uniqueId)
              .name(name)
              .email(email)
              .department(department)
              .major1(major1)
              .major2(major2)
              .grade(grade)
              .semester(semester)
              .level(level)
              .build();
    } catch (HttpStatusCodeException e) {
      Map<String, Object> result = new HashMap<>();
        try {
            result = new ObjectMapper().readValue(e.getResponseBodyAsString(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            throw new FailedHisnetLoginException("예상치 못한 변수 발생", 500);
        }
      throw new FailedHisnetLoginException(
          result.get("message").toString(), e.getStatusCode().value());
    }
  }
}
