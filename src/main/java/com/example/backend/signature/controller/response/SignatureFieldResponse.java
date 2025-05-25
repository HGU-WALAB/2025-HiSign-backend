package com.example.backend.signature.controller.response;

import com.example.backend.signature.DTO.SignatureDTO;

import java.util.List;

public class SignatureFieldResponse {
    private boolean hasExistingSignature;
    private List<SignatureDTO> fields;

    public SignatureFieldResponse(boolean hasExistingSignature, List<SignatureDTO> fields) {
        this.hasExistingSignature = hasExistingSignature;
        this.fields = fields;
    }

    public boolean isHasExistingSignature() {
        return hasExistingSignature;
    }

    public List<SignatureDTO> getFields() {
        return fields;
    }
}
