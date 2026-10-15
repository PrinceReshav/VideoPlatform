package com.platform.video.repository;

import com.platform.common.entity.VideoFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VideoFileRepository extends JpaRepository<VideoFile, UUID> {

    List<VideoFile> findByVideoId(UUID videoId);

    Optional<VideoFile> findByVideoIdAndFileTypeAndMaster(UUID videoId, com.platform.common.entity.FileType fileType, boolean isMaster);

    List<VideoFile> findByVideoIdAndFileType(UUID videoId, com.platform.common.entity.FileType fileType);

    List<VideoFile> findByVideoIdAndFileTypeIn(UUID videoId, List<com.platform.common.entity.FileType> fileTypes);

    @Query("SELECT vf FROM VideoFile vf WHERE vf.videoId = :videoId AND vf.fileType = 'HLS_MANIFEST' AND vf.master = true")
    Optional<VideoFile> findMasterManifest(@Param("videoId") UUID videoId);

    @Query("SELECT vf FROM VideoFile vf WHERE vf.videoId = :videoId AND vf.fileType = 'THUMBNAIL' ORDER BY vf.createdAt ASC")
    List<VideoFile> findThumbnails(@Param("videoId") UUID videoId);

    void deleteByVideoId(UUID videoId);
}