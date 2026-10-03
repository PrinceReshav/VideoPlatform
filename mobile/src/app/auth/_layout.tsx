import React from 'react';
import { View, Text, StyleSheet, ActivityIndicator } from 'react-native';
import { Slot } from 'expo-router';
import { Stack } from 'expo-router';
import { AuthProvider } from '../providers/AuthProvider';
import { ThemeProvider } from '../providers/ThemeProvider';

export default function AuthLayout() {
  return (
    <AuthProvider>
      <ThemeProvider>
        <Stack screenOptions={{ headerShown: false }}>
          <Slot />
        </Stack>
      </ThemeProvider>
    </AuthProvider>
  );
}