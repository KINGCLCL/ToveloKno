package backend.backend.question;

/**
 * 题型编码。
 *
 * 接口使用稳定的英文枚举值，前端负责展示对应的中文名称。
 */
public enum QuestionType {
    SINGLE_CHOICE,
    MULTIPLE_CHOICE,
    TRUE_FALSE,
    FILL_BLANK,
    SHORT_ANSWER
}
