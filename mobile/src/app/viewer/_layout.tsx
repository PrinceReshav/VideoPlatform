import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { Tabs } from 'expo-router';
import { Home, Search, Users, Clock, List, User, Tv, Radio } from 'lucide-react-native';
import { useAuth } from '../providers/AuthProvider';
import { ThemeProvider } from '../providers/ThemeProvider';

export default function ViewerLayout() {
  const { isAuthenticated } = useAuth();

  if (!isAuthenticated) {
    return (
      <View style={styles.container}>
        <Text>Please sign in</Text>
      </View>
    );
  }

  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: '#6366f1',
        tabBarInactiveTintColor: '#94a3b8',
        tabBarStyle: {
          backgroundColor: '#ffffff',
          borderTopWidth: 0,
          elevation: 8,
          shadowColor: '#000',
          shadowOffset: { width: 0, height: -2 },
          shadowOpacity: 0.1,
          shadowRadius: 4,
        },
      }}
    >
      <Tabs.Screen
        name="viewer/home"
        options={{
          title: 'Home',
          tabBarIcon: ({ focused }) => <Home size={24} color={focused ? '#6366f1' : '#94a3b8'} />,
        }}
      />
      <Tabs.Screen
        name="viewer/live"
        options={{
          title: 'Live TV',
          tabBarIcon: ({ focused }) => <Tv size={24} color={focused ? '#6366f1' : '#94a3b8'} />,
        }}
      />
      <Tabs.Screen
        name="viewer/search"
        options={{
          title: 'Search',
          tabBarIcon: ({ focused }) => <Search size={24} color={focused ? '#6366f1' : '#94a3b8'} />,
        }}
      />
      <Tabs.Screen
        name="viewer/subscriptions"
        options={{
          title: 'Subscriptions',
          tabBarIcon: ({ focused }) => <Users size={24} color={focused ? '#6366f1' : '#94a3b8'} />,
        }}
      />
      <Tabs.Screen
        name="viewer/library"
        options={{
          title: 'Library',
          tabBarIcon: ({ focused }) => <List size={24} color={focused ? '#6366f1' : '#94a3b8'} />,
        }}
      />
      <Tabs.Screen
        name="viewer/profile"
        options={{
          title: 'Profile',
          tabBarIcon: ({ focused }) => <User size={24} color={focused ? '#6366f1' : '#94a3b8'} />,
        }}
      />
    </Tabs>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
});