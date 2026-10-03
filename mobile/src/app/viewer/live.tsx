<Image
          source={{ uri: channel.logo || channel.tvgLogo }}
          style={[styles.thumbnail, compact && styles.thumbnailCompact]}
          resizeMode="cover"
          onError={() => {
            const initial = channel.name && channel.name.length > 0 ? channel.name.charAt(0) : '?';
            // Note: In react-native, we need to use a different approach for fallback images
            // For now, we'll just log the error and show a placeholder
            console.error('Failed to load channel image:', channel.name || 'unknown');
          }}
        />