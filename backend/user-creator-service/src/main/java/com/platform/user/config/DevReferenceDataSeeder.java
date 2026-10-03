package com.platform.user.config;

import com.platform.user.entity.Category;
import com.platform.user.entity.Channel;
import com.platform.user.entity.CreatorProfile;
import com.platform.user.entity.Genre;
import com.platform.user.entity.Language;
import com.platform.user.repository.CategoryRepository;
import com.platform.user.repository.ChannelRepository;
import com.platform.user.repository.CreatorProfileRepository;
import com.platform.user.repository.GenreRepository;
import com.platform.user.repository.LanguageRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.UUID;

@Configuration
@Profile("dev")
public class DevReferenceDataSeeder {
    @Bean
    CommandLineRunner seedReferenceData(CategoryRepository categories, GenreRepository genres,
                                        LanguageRepository languages, CreatorProfileRepository creators,
                                        ChannelRepository channels) {
        return args -> {
            Category technology = category(categories, "Technology", "technology", "Technology, software and innovation", "💻", "#06b6d4", 1);
            category(categories, "Music", "music", "Music and performances", "🎵", "#ec4899", 2);
            category(categories, "Gaming", "gaming", "Games and live gameplay", "🎮", "#8b5cf6", 3);
            category(categories, "Education", "education", "Courses and learning", "🎓", "#22c55e", 4);
            category(categories, "Entertainment", "entertainment", "Entertainment and culture", "🎬", "#f97316", 5);
            category(categories, "Sports", "sports", "Sports and fitness", "🏆", "#ef4444", 6);
            category(categories, "Podcasts", "podcasts", "Podcasts and conversations", "🎙️", "#eab308", 7);

            genre(genres, "Programming", "programming", 1);
            genre(genres, "Artificial Intelligence", "artificial-intelligence", 2);
            genre(genres, "Devotional", "devotional", 3);
            genre(genres, "Comedy", "comedy", 4);
            genre(genres, "Documentary", "documentary", 5);
            genre(genres, "News", "news", 6);
            genre(genres, "Travel", "travel", 7);
            genre(genres, "Finance", "finance", 8);

            language(languages, "en", "English", "English");
            language(languages, "hi", "Hindi", "हिन्दी");
            language(languages, "bn", "Bengali", "বাংলা");
            language(languages, "ta", "Tamil", "தமிழ்");
            language(languages, "te", "Telugu", "తెలుగు");

            if (creators.findByUserId(UUID.fromString("00000000-0000-0000-0000-000000000002")).isEmpty()) {
                CreatorProfile profile = new CreatorProfile();
                profile.setId(UUID.fromString("00000000-0000-0000-0000-000000000010"));
                profile.setUserId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
                profile.setChannelName("VideoPlatform Studio");
                profile.setChannelSlug("videoplatform-studio");
                profile.setDescription("Demo creator channel for local development.");
                profile.setVerified(true);
                profile.setActive(true);
                profile.setDefaultLanguageId(languages.findByCode("en").orElseThrow().getId());
                creators.save(profile);

                Channel channel = new Channel();
                channel.setId(UUID.fromString("00000000-0000-0000-0000-000000000011"));
                channel.setCreatorProfileId(profile.getId());
                channel.setName("VideoPlatform Studio");
                channel.setSlug("videoplatform-studio");
                channel.setDescription("Demo channel for local development.");
                channel.setVerified(true);
                channel.setActive(true);
                channel.setCategory(technology);
                channel.setDefaultLanguageId(profile.getDefaultLanguageId());
                channel.setCountry("IN");
                channels.save(channel);
            }
        };
    }

    private Category category(CategoryRepository r, String name, String slug, String desc, String icon, String color, int order) {
        return r.findBySlug(slug).orElseGet(() -> { Category c=new Category(); c.setName(name); c.setSlug(slug); c.setDescription(desc); c.setIcon(icon); c.setColor(color); c.setSortOrder(order); c.setSystem(true); c.setActive(true); return r.save(c); });
    }
    private Genre genre(GenreRepository r, String name, String slug, int order) {
        return r.findBySlug(slug).orElseGet(() -> { Genre g=new Genre(); g.setName(name); g.setSlug(slug); g.setSortOrder(order); g.setSystem(true); g.setActive(true); return r.save(g); });
    }
    private Language language(LanguageRepository r, String code, String name, String nativeName) {
        return r.findByCode(code).orElseGet(() -> { Language l=new Language(); l.setCode(code); l.setName(name); l.setNativeName(nativeName); l.setSystem(true); l.setActive(true); return r.save(l); });
    }
}
