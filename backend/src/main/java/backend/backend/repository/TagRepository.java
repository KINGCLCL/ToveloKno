package backend.backend.repository;

import backend.backend.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 标签数据访问层。
 *
 * 标签名称全局唯一，所以这里提供按名称判重和关键词查询。
 */
public interface TagRepository extends JpaRepository<Tag, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    List<Tag> findByNameContainingIgnoreCaseOrderByNameAsc(String keyword);

    List<Tag> findAllByOrderByNameAsc();
}
