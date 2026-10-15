package com.platform.livetv.repository;

import com.platform.livetv.dto.CategoryStats;
import com.platform.livetv.dto.CountryStats;
import com.platform.livetv.entity.LiveChannel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LiveChannelRepository extends JpaRepository<LiveChannel, UUID> {

    /*
     * ============================================================
     * BASIC FILTER QUERIES
     * ============================================================
     */

    Page<LiveChannel> findByCountryCodeAndDeletedFalse(
            String countryCode,
            Pageable pageable
    );

    @Query("""
            SELECT DISTINCT c FROM LiveChannel c JOIN c.categories cat
            WHERE c.deleted = false AND LOWER(cat) = LOWER(:category)
            """)
    Page<LiveChannel> findByCategoryAndDeletedFalse(
            @Param("category") String category, Pageable pageable
    );

    Page<LiveChannel> findByLanguageAndDeletedFalse(
            String language,
            Pageable pageable
    );

    Page<LiveChannel> findByStatusAndDeletedFalse(
            LiveChannel.StreamStatus status,
            Pageable pageable
    );

    Page<LiveChannel> findByFavoriteTrueAndDeletedFalse(
            Pageable pageable
    );


    /*
     * ============================================================
     * SEARCH
     * ============================================================
     *
     * Searches channel name, country and category.
     *
     * Soft-deleted channels are always excluded.
     */

    @Query("""
            SELECT c
            FROM LiveChannel c
            WHERE c.deleted = false
              AND (
                    LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(c.country) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(c.category) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            """)
    Page<LiveChannel> search(
            @Param("search") String search,
            Pageable pageable
    );


    /*
     * ============================================================
     * COMBINED FILTER
     * ============================================================
     *
     * Supports:
     *   - country
     *   - category
     *   - search
     *
     * All filters are optional.
     *
     * The service normalizes blank values to null before calling
     * this method.
     */

    @Query("""
            SELECT c
            FROM LiveChannel c
            WHERE c.deleted = false

              AND (:country IS NULL OR c.countryCode = :country)
              AND (:category IS NULL OR EXISTS (SELECT 1 FROM c.categories cat WHERE LOWER(cat) = LOWER(:category)))

              AND (
                    :search IS NULL
                    OR TRIM(:search) = ''
                    OR LOWER(c.name) LIKE
                       LOWER(CONCAT('%', TRIM(:search), '%'))
                    OR LOWER(c.country) LIKE
                       LOWER(CONCAT('%', TRIM(:search), '%'))
                    OR LOWER(c.category) LIKE
                       LOWER(CONCAT('%', TRIM(:search), '%'))
              )
            """)
    Page<LiveChannel> findWithFilters(
            @Param("country") String country,
            @Param("category") String category,
            @Param("search") String search,
            Pageable pageable
    );


    /*
     * ============================================================
     * ORDERED LIST QUERIES
     * ============================================================
     */

    List<LiveChannel> findByCountryCodeAndDeletedFalseOrderByNameAsc(
            String countryCode
    );

    List<LiveChannel> findByCategoryAndDeletedFalseOrderByNameAsc(
            String category
    );

    List<LiveChannel> findByFavoriteTrueAndDeletedFalseOrderByViewCountDesc();


    /*
     * ============================================================
     * HEALTH CHECK SUPPORT
     * ============================================================
     */

    List<LiveChannel> findAllByStatusInAndDeletedFalse(
            List<LiveChannel.StreamStatus> statuses, Pageable pageable
    );


    /*
     * ============================================================
     * CHANNEL LOOKUPS
     * ============================================================
     */

    Optional<LiveChannel> findByTvgIdAndDeletedFalse(
            String tvgId
    );

    Optional<LiveChannel> findByTvgId(String tvgId);

    List<LiveChannel> findAllByDeletedFalse();

    Optional<LiveChannel> findByIdAndDeletedFalse(
            UUID id
    );


    /*
     * ============================================================
     * DISTINCT FILTER VALUES
     * ============================================================
     */

    @Query("""
            SELECT DISTINCT c.countryCode
            FROM LiveChannel c
            WHERE c.deleted = false
            ORDER BY c.countryCode
            """)
    List<String> findDistinctCountryCodesByDeletedFalse();


    @Query("""
            SELECT DISTINCT c.category
            FROM LiveChannel c
            WHERE c.deleted = false
            ORDER BY c.category
            """)
    List<String> findDistinctCategoriesByDeletedFalse();


    @Query("""
            SELECT DISTINCT c.language
            FROM LiveChannel c
            WHERE c.deleted = false
            ORDER BY c.language
            """)
    List<String> findDistinctLanguagesByDeletedFalse();


    /*
     * ============================================================
     * COUNTRY / CATEGORY STATISTICS
     * ============================================================
     */

    @Query("""
            SELECT NEW com.platform.livetv.dto.CountryStats(
                c.countryCode,
                c.country,
                COUNT(c)
            )
            FROM LiveChannel c
            WHERE c.deleted = false
              AND (:category IS NULL OR EXISTS (SELECT 1 FROM c.categories cat WHERE LOWER(cat) = LOWER(:category)))
              AND (:search IS NULL OR TRIM(:search) = ''
                   OR LOWER(c.name) LIKE LOWER(CONCAT('%', TRIM(:search), '%'))
                   OR LOWER(c.country) LIKE LOWER(CONCAT('%', TRIM(:search), '%'))
                   OR LOWER(c.category) LIKE LOWER(CONCAT('%', TRIM(:search), '%')))
            GROUP BY c.countryCode, c.country
            ORDER BY COUNT(c) DESC, c.country ASC
            """)
    List<CountryStats> getCountryStats(
            @Param("category") String category,
            @Param("search") String search
    );


    @Query("""
            SELECT NEW com.platform.livetv.dto.CategoryStats(
                cat,
                COUNT(DISTINCT c)
            )
            FROM LiveChannel c JOIN c.categories cat
            WHERE c.deleted = false
              AND (:country IS NULL OR c.countryCode = :country)
              AND (:search IS NULL OR TRIM(:search) = ''
                   OR LOWER(c.name) LIKE LOWER(CONCAT('%', TRIM(:search), '%'))
                   OR LOWER(c.country) LIKE LOWER(CONCAT('%', TRIM(:search), '%'))
                   OR LOWER(cat) LIKE LOWER(CONCAT('%', TRIM(:search), '%')))
            GROUP BY cat
            ORDER BY COUNT(DISTINCT c) DESC, cat ASC
            """)
    List<CategoryStats> getCategoryStats(
            @Param("country") String country,
            @Param("search") String search
    );


    /*
     * ============================================================
     * COUNTS
     * ============================================================
     */

    long countByStatusAndDeletedFalse(
            LiveChannel.StreamStatus status
    );

    long countByDeletedFalse();


    /*
     * ============================================================
     * ONLINE / FEATURED CHANNELS
     * ============================================================
     *
     * Do not hard-code:
     *
     *     c.status = 'ONLINE'
     *
     * Instead use a strongly typed enum derived query.
     */

    List<LiveChannel> findByStatusAndDeletedFalseOrderByViewCountDesc(
            LiveChannel.StreamStatus status,
            Pageable pageable
    );


    /*
     * Kept with the exact method name required by
     * IptvOrgSyncService:
     *
     *     channelRepository.findTopOnlineChannels(...)
     *
     * This default method delegates to the type-safe Spring
     * Data derived query above.
     */
    default List<LiveChannel> findTopOnlineChannels(
            Pageable pageable
    ) {
        return findByStatusAndDeletedFalseOrderByViewCountDesc(
                LiveChannel.StreamStatus.PLAYABLE, pageable
        );
    }
}