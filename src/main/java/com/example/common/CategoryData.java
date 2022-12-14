package com.example.common;

import java.util.ArrayList;
import java.util.List;

public class CategoryData {
    /**
     * 기본 카테고리 설정 리스트(26개)
     */
    public static final List<String> categoryNames = new ArrayList<>();
    static {
        categoryNames.add("건강");
        categoryNames.add("경제경영");
        categoryNames.add("고등학교참고서");
        categoryNames.add("과학");
        categoryNames.add("대학교재");
        categoryNames.add("만화");
        categoryNames.add("사회과학");
        categoryNames.add("소설");
        categoryNames.add("수험서");
        categoryNames.add("어린이");
        categoryNames.add("에세이");
        categoryNames.add("여행");
        categoryNames.add("역사");
        categoryNames.add("예술");
        categoryNames.add("외국어");
        categoryNames.add("요리");
        categoryNames.add("유아");
        categoryNames.add("인문학");
        categoryNames.add("자기계발");
        categoryNames.add("잡지");
        categoryNames.add("종교");
        categoryNames.add("좋은부모");
        categoryNames.add("중학교참고서");
        categoryNames.add("청소년");
        categoryNames.add("초등학교참고서");
        categoryNames.add("컴퓨터");
    }
}
