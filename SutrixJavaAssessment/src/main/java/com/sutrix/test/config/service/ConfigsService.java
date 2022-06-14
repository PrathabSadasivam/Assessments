package com.sutrix.test.config.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sutrix.test.config.model.Configs;
import com.sutrix.test.config.model.JsonFields;

@Service
public class ConfigsService {
	private Configs configs;
	private Map<String, JsonFields> jsonMap = new HashMap();
	private Map<String, JsonFields> csvMap = new HashMap();
	ObjectMapper mapper = new ObjectMapper();

	@PostConstruct
	public void init() {
		loadCSV();
	}

	public JsonFields getJosnFieldNameFromConfig(String fileFieldName) {
		return !csvMap.isEmpty() ? csvMap.get(fileFieldName) : null;

	}
	
	public JsonFields getFileFieldNameFromConfig(String fileFieldName) {
		return !csvMap.isEmpty() ? jsonMap.get(fileFieldName) : null;

	}
	
	public void loadCSV() {
		try {
			Resource resource = new ClassPathResource("config.properties");
			configs = mapper.readValue(new File(resource.getFile().getAbsolutePath()), Configs.class);

			jsonMap = org.apache.commons.collections4.CollectionUtils.emptyIfNull(configs.getConfigs()).stream()
					.collect(Collectors.toMap(JsonFields::getCsv_attr, Function.identity()));

			csvMap = org.apache.commons.collections4.CollectionUtils.emptyIfNull(configs.getConfigs()).stream()
					.collect(Collectors.toMap(JsonFields::getCsv_attr, Function.identity()));

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
