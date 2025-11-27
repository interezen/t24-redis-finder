package com.interezen.t24.redis_finder.utils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by jkpark on 2018-12-31.
 */
public class PacketUtils {
	public static Map<String, String> kv(String data, String fieldSplit, String valueSplit) throws Exception {
		String[] splitData = data.split(fieldSplit);
		String[] valueData;
		StringBuilder value = null;
		int size = splitData == null ? 0 : splitData.length;
		int valueSize;

		Map<String, String> result = new LinkedHashMap<>();
		for(int i=0; i<size; i++) {
			valueData = splitData[i].split(valueSplit, 2);
			valueSize = valueData.length;

			if(valueSize == 1) {
				// key는 있으나 value가 없는 field는 무시한다.
//                result.put(valueData[0].trim(), "");
			} else if(valueSize == 2) {
				// key는 있으나 value가 없는 field는 무시한다. (이곳은 공백 대비)
				if(!valueData[1].trim().isEmpty())
					result.put(valueData[0].trim(), valueData[1].trim());
			} else if(valueSize > 2) {
				// 원문 value 영역에 구분자를 포함한 데이터가 있을 경우 원문 value 영역 모두를 value로 셋팅한다.
				value = new StringBuilder();

				for(int j = 1; j < valueSize; j++) {
					value.append(valueData[j].trim());
					if(j < (valueSize - 1)) {
						value.append(valueSplit);
					}
				}
				
				result.put(valueData[0].trim(), value.toString());
			}
		}

		return result;
	}
}