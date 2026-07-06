package backend.backend.repository;

import backend.backend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 分类数据访问层。
 *
 * 分类模块主要需要按名称、父分类查询，用于判断重复和展示分类树。
 */
@Repository("categoryManagementRepository")
public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByNameAndParentId(String name, Long parentId);

    boolean existsByNameAndParentIdAndIdNot(String name, Long parentId, Long id);

    List<Category> findByNameContainingIgnoreCaseOrderByNameAsc(String keyword);

    List<Category> findAllByOrderByNameAsc();

    List<Category> findByParentId(Long parentId);
}
