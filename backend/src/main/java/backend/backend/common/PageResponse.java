package backend.backend.common;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 分页接口统一返回结构。
 *
 * 业务模块使用 Spring Data JPA 查询出 Page<T> 后，可以用 PageResponse.from(pageData)
 * 转成前端更容易消费的 records、total、page、size、totalPages 格式。
 */
public class PageResponse<T> {

    private List<T> records;
    private long total;
    private int page;
    private int size;
    private int totalPages;

    public PageResponse() {
    }

    public PageResponse(List<T> records, long total, int page, int size, int totalPages) {
        this.records = records;
        this.total = total;
        this.page = page;
        this.size = size;
        this.totalPages = totalPages;
    }

    /**
     * 将 Spring Data 的 Page 对象转成统一分页响应。
     *
     * 注意：前端页码从 1 开始展示，所以这里把 Page#getNumber() 的 0 基页码加 1。
     */
    public static <T> PageResponse<T> from(Page<T> pageData) {
        return new PageResponse<>(
                pageData.getContent(),
                pageData.getTotalElements(),
                pageData.getNumber() + 1,
                pageData.getSize(),
                pageData.getTotalPages()
        );
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}
