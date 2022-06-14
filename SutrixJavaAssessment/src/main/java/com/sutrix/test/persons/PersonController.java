package com.sutrix.test.persons;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sutrix.test.config.model.JsonFields;
import com.sutrix.test.config.service.ConfigsService;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

@RestController
public class PersonController {
	@Autowired
	ConfigsService configsService;
	ObjectMapper mapper = new ObjectMapper();

	@PostMapping(value = "/createPerson")
	public String createPerson(@RequestBody String person) throws ParseException {
		Resource resource = new ClassPathResource("persons.csv");
		String id = StringUtils.EMPTY;
		boolean flag = true;
		try {
			CSVWriter writer = new CSVWriter(new FileWriter(resource.getFile().getAbsolutePath(), true));
			JSONParser parser = new JSONParser();
			JSONObject json = (JSONObject) parser.parse(person);
			CSVReader reader = new CSVReader(new FileReader(resource.getFile()), ',', '"', 0);
			List<String> fieldNames = Arrays.asList(reader.readNext());
			String[] record = new String[fieldNames.size()];
			int i = 0;
			for (String fieldName : fieldNames) {
				if (flag && (fieldName.equalsIgnoreCase("First Name") || fieldName.equalsIgnoreCase("Last Name"))) {
					flag = ((String) json.get(fieldName)).matches("^[A-Za-z\\s]{1,}[\\.]{0,1}[A-Za-z\\s]{0,}$");
				}
				if (flag && fieldName.equalsIgnoreCase("Email")) {
					flag = Pattern.compile("^(.+)@(\\S+)$").matcher((String) json.get(fieldName)).matches();
				}
				if (fieldName.equalsIgnoreCase("id")) {
					id = System.currentTimeMillis() + "";
					record[i++] = id;
				} else {
					record[i++] = (String) json.get(fieldName);
				}
			}
			if (flag)
				writer.writeNext(record);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		configsService.loadCSV();
		return getPersonJson(id);
	}

	@GetMapping("/getPerson/{id}")
	public String getPersonById(@PathVariable("id") String id) {
		return getPersonJson(id);
	}

	@GetMapping("/getPersons")
	public String getPersons() {
		return getPersonJson(StringUtils.EMPTY);
	}

	private String getPersonJson(String reqId) {
		List<Map<String, Object>> list = null;
		String personJon = StringUtils.EMPTY;
		try {
			Resource resource = new ClassPathResource("persons.csv");
			@SuppressWarnings("resource")
			CSVReader reader = new CSVReader(new FileReader(resource.getFile()), ',', '"', 0);
			List<String> fieldNames = Arrays.asList(reader.readNext());
			list = new ArrayList<>();
			String[] nextLine;
			while ((nextLine = reader.readNext()) != null) {
				boolean addToList = true;
				List<String> x = Arrays.asList(nextLine);
				Map<String, Object> obj = new LinkedHashMap<>();
				for (int i = 0; i < fieldNames.size(); i++) {
					if (StringUtils.isNotBlank(reqId) && "id".equalsIgnoreCase(fieldNames.get(i))) {
						if(StringUtils.isBlank(x.get(i)) || !reqId.equals(x.get(i))) {
							addToList = false;
						}
					}
					JsonFields jsonField = configsService.getJosnFieldNameFromConfig(fieldNames.get(i));
					if (jsonField != null) {
						if (jsonField.getType().equalsIgnoreCase("Int") && StringUtils.isNumeric(x.get(i))) {
							obj.put(jsonField.getJson_attr(), Integer.parseInt(x.get(i)));
						} else {
							obj.put(jsonField.getJson_attr(), x.get(i));
						}
					}

				}
				if (addToList)
					list.add(obj);
			}
			mapper.enable(SerializationFeature.INDENT_OUTPUT);
			personJon = mapper.writeValueAsString(list);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return personJon;
	}

}
