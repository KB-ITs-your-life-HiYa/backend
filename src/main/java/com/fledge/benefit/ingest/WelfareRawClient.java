//한국사회보장정보원_중앙부처복지서비스
//XML 파싱
package com.fledge.benefit.ingest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class WelfareRawClient {

    private static final String BASE_URL = "https://apis.data.go.kr/B554287/NationalWelfareInformationsV001";

    // 기본값을 두는 이유: 이 빈은 조건 없이 항상 생성된다.
    // 값이 없으면 키를 갖지 않은 팀원은 앱 자체를 기동할 수 없다.
    // 실제 호출은 수집 프로필(ingest/parse)에서만 일어난다.
    @Value("${welfare.service-key:}")
    private String serviceKey;

    private final RestClient restClient = RestClient.create(BASE_URL);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<RawSubsidy> fetchList(String keyword, int pageNo, int numOfRows) {
        String xml = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/NationalWelfarelistV001")
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("callTp", "L")
                        .queryParam("pageNo", pageNo)
                        .queryParam("numOfRows", numOfRows)
                        .queryParam("srchKeyCode", "003")
                        .queryParam("searchWrd", keyword)
                        .build())
                .retrieve()
                .body(String.class);

        return parseServList(xml);
    }

    private List<RawSubsidy> parseServList(String xml) {
        List<RawSubsidy> result = new ArrayList<>();
        try {
            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new InputSource(new StringReader(xml)));

            NodeList servListNodes = doc.getElementsByTagName("servList");
            for (int i = 0; i < servListNodes.getLength(); i++) {
                Map<String, String> record = elementToMap((Element) servListNodes.item(i));
                result.add(new RawSubsidy(record.get("servId"), objectMapper.writeValueAsString(record)));
            }
        } catch (Exception e) {
            throw new IllegalStateException("복지로 응답 파싱 실패", e);
        }
        return result;
    }

    private Map<String, String> elementToMap(Element element) {
        Map<String, String> map = new LinkedHashMap<>();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                map.put(child.getNodeName(), child.getTextContent());
            }
        }
        return map;
    }
}