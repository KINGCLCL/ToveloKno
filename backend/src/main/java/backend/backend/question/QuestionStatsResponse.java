package backend.backend.question;

/**
 * 题目管理顶部统计卡片数据。
 */
public class QuestionStatsResponse {

    private final long total;
    private final long published;
    private final long draft;
    private final long recycleBin;

    public QuestionStatsResponse(long total, long published, long draft, long recycleBin) {
        this.total = total;
        this.published = published;
        this.draft = draft;
        this.recycleBin = recycleBin;
    }

    public long getTotal() {
        return total;
    }

    public long getPublished() {
        return published;
    }

    public long getDraft() {
        return draft;
    }

    public long getRecycleBin() {
        return recycleBin;
    }
}
