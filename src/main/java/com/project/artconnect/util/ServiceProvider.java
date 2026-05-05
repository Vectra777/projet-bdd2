package com.project.artconnect.util;

import com.project.artconnect.config.DatabaseConfig;
import com.project.artconnect.dao.ArtistDao;
import com.project.artconnect.dao.ArtworkDao;
import com.project.artconnect.dao.CommunityMemberDao;
import com.project.artconnect.dao.GalleryDao;
import com.project.artconnect.dao.WorkshopDao;
import com.project.artconnect.persistence.JdbcArtistDao;
import com.project.artconnect.persistence.JdbcArtworkDao;
import com.project.artconnect.persistence.JdbcCommunityMemberDao;
import com.project.artconnect.persistence.JdbcDisciplineDao;
import com.project.artconnect.persistence.JdbcGalleryDao;
import com.project.artconnect.persistence.JdbcWorkshopDao;
import com.project.artconnect.service.*;
import com.project.artconnect.service.impl.*;

import java.sql.SQLException;

/**
 * Service Provider to manage singleton instances of services and handle their
 * initialization.
 */
public class ServiceProvider {
    private static final ArtistService artistService;
    private static final ArtworkService artworkService;
    private static final GalleryService galleryService;
    private static final WorkshopService workshopService;
    private static final CommunityService communityService;

    static {
        Services services = initializeServices();
        artistService = services.artistService();
        artworkService = services.artworkService();
        galleryService = services.galleryService();
        workshopService = services.workshopService();
        communityService = services.communityService();
    }

    public static ArtistService getArtistService() {
        return artistService;
    }

    public static ArtworkService getArtworkService() {
        return artworkService;
    }

    public static GalleryService getGalleryService() {
        return galleryService;
    }

    public static WorkshopService getWorkshopService() {
        return workshopService;
    }

    public static CommunityService getCommunityService() {
        return communityService;
    }

    private static Services initializeServices() {
        if (DatabaseConfig.JDBC_ENABLED) {
            try (var ignored = ConnectionManager.getConnection()) {
                return createJdbcServices();
            } catch (SQLException | RuntimeException e) {
                System.err.println("JDBC initialization failed, fallback to in-memory services: " + e.getMessage());
            }
        }
        return createInMemoryServices();
    }

    private static Services createJdbcServices() {
        ArtistDao artistDao = new JdbcArtistDao();
        ArtworkDao artworkDao = new JdbcArtworkDao();
        GalleryDao galleryDao = new JdbcGalleryDao();
        WorkshopDao workshopDao = new JdbcWorkshopDao();
        CommunityMemberDao communityMemberDao = new JdbcCommunityMemberDao();
        JdbcDisciplineDao disciplineDao = new JdbcDisciplineDao();

        return new Services(
                new JdbcArtistService(artistDao, disciplineDao),
                new JdbcArtworkService(artworkDao),
                new JdbcGalleryService(galleryDao),
                new JdbcWorkshopService(workshopDao),
                new JdbcCommunityService(communityMemberDao));
    }

    private static Services createInMemoryServices() {
        InMemoryArtistService artistService = new InMemoryArtistService();
        InMemoryArtworkService artworkService = new InMemoryArtworkService();
        InMemoryGalleryService galleryService = new InMemoryGalleryService();
        InMemoryWorkshopService workshopService = new InMemoryWorkshopService();
        InMemoryCommunityService communityService = new InMemoryCommunityService();

        artworkService.initData(artistService);
        galleryService.initData(artworkService);
        workshopService.initData(artistService);
        communityService.initData(artworkService);

        return new Services(artistService, artworkService, galleryService, workshopService, communityService);
    }

    private record Services(
            ArtistService artistService,
            ArtworkService artworkService,
            GalleryService galleryService,
            WorkshopService workshopService,
            CommunityService communityService) {
    }
}
