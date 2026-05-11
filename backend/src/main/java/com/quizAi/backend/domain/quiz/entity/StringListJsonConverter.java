package com.quizAi.backend.domain.quiz.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Collections;
import java.util.List;

@Converter
public class StringListJsonConverter implements AttributeConverter<List<String>, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute == null ? Collections.emptyList() : attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("보기 데이터를 저장할 수 없습니다.", e);
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        try {
            return dbData == null
                    ? Collections.emptyList()
                    : OBJECT_MAPPER.readValue(dbData, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("보기 데이터를 읽을 수 없습니다.", e);
        }
    }
}
