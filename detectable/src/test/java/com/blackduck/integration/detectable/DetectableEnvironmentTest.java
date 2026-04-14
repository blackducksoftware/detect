package com.blackduck.integration.detectable;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class DetectableEnvironmentTest {
    
    @Test
    public void testDirectoryCacheMemoizesListFiles() {
        // Create a mock directory to track method calls
        File mockDirectory = Mockito.mock(File.class);
        File[] expectedFiles = new File[] { new File("file1.txt"), new File("file2.txt") };
        
        // Mock the listFiles method to return our expected files
        when(mockDirectory.listFiles()).thenReturn(expectedFiles);
        when(mockDirectory.getAbsolutePath()).thenReturn("/fake/path");
        
        // Test the DirectoryCache directly since it's package-visible
        DetectableEnvironment.DirectoryCache cache = new DetectableEnvironment.DirectoryCache(mockDirectory);
        
        // Call listFiles multiple times
        File[] files1 = cache.listFiles();
        File[] files2 = cache.listFiles();
        File[] files3 = cache.listFiles();
        
        // Verify the underlying listFiles was called only once
        verify(mockDirectory, times(1)).listFiles();
        
        // Verify all calls return the same cached result
        assertSame(files1, files2);
        assertSame(files2, files3);
        assertArrayEquals(expectedFiles, files1);
    }
    
    @Test
    public void testDirectoryCacheMemoizesIsDirectoryTrue() {
        // Create a mock directory to track method calls
        File mockDirectory = Mockito.mock(File.class);
        
        // Mock the isDirectory method to return true
        when(mockDirectory.isDirectory()).thenReturn(true);
        when(mockDirectory.getAbsolutePath()).thenReturn("/fake/path");
        
        // Test the DirectoryCache directly
        DetectableEnvironment.DirectoryCache cache = new DetectableEnvironment.DirectoryCache(mockDirectory);
        
        // Call isDirectory multiple times
        boolean isDir1 = cache.isDirectory();
        boolean isDir2 = cache.isDirectory();
        boolean isDir3 = cache.isDirectory();
        
        // Verify the underlying isDirectory was called only once
        verify(mockDirectory, times(1)).isDirectory();
        
        // Verify all calls return the same cached result
        assertTrue(isDir1);
        assertTrue(isDir2);
        assertTrue(isDir3);
    }
    
    @Test
    public void testDirectoryCacheMemoizesIsDirectoryFalse() {
        // Create a mock directory to track method calls
        File mockDirectory = Mockito.mock(File.class);
        
        // Mock the isDirectory method to return false
        when(mockDirectory.isDirectory()).thenReturn(false);
        when(mockDirectory.getAbsolutePath()).thenReturn("/fake/path");
        
        // Test the DirectoryCache directly
        DetectableEnvironment.DirectoryCache cache = new DetectableEnvironment.DirectoryCache(mockDirectory);
        
        // Call isDirectory multiple times
        boolean isDir1 = cache.isDirectory();
        boolean isDir2 = cache.isDirectory();
        boolean isDir3 = cache.isDirectory();
        
        // Verify the underlying isDirectory was called only once
        verify(mockDirectory, times(1)).isDirectory();
        
        // Verify all calls return the same cached result
        assertFalse(isDir1);
        assertFalse(isDir2);
        assertFalse(isDir3);
    }
    
    @Test
    public void testDirectoryCacheHandlesNullListFiles() {
        // Create a mock directory to track method calls
        File mockDirectory = Mockito.mock(File.class);
        
        // Mock the listFiles method to return null (e.g., not a directory or IO error)
        when(mockDirectory.listFiles()).thenReturn(null);
        when(mockDirectory.getAbsolutePath()).thenReturn("/fake/path");
        
        // Test the DirectoryCache directly
        DetectableEnvironment.DirectoryCache cache = new DetectableEnvironment.DirectoryCache(mockDirectory);
        
        // Call listFiles multiple times
        File[] files1 = cache.listFiles();
        File[] files2 = cache.listFiles();
        
        // Verify the underlying listFiles was called only once
        verify(mockDirectory, times(1)).listFiles();
        
        // Verify all calls return the same cached result (null)
        assertNull(files1);
        assertNull(files2);
    }
    
    @Test
    public void testWrappedDirectoryDelegatestoCache() {
        // Create a mock directory
        File mockDirectory = Mockito.mock(File.class);
        File[] expectedFiles = new File[] { new File("file1.txt") };
        
        when(mockDirectory.listFiles()).thenReturn(expectedFiles);
        when(mockDirectory.isDirectory()).thenReturn(true);
        when(mockDirectory.getAbsolutePath()).thenReturn("/fake/path");
        
        DetectableEnvironment environment = new DetectableEnvironment(mockDirectory);
        File wrappedDirectory = environment.getDirectory();
        
        // Call methods on the wrapped directory
        File[] files1 = wrappedDirectory.listFiles();
        File[] files2 = wrappedDirectory.listFiles();
        boolean isDir1 = wrappedDirectory.isDirectory();
        boolean isDir2 = wrappedDirectory.isDirectory();
        
        // Verify memoization works through the wrapper
        verify(mockDirectory, times(1)).listFiles();
        verify(mockDirectory, times(1)).isDirectory();
        
        assertSame(files1, files2);
        assertEquals(isDir1, isDir2);
        assertTrue(isDir1);
    }
    
    @Test
    public void testCachesAreSeparateForDifferentDirectories() {
        // Create two different mock directories
        File mockDirectory1 = Mockito.mock(File.class);
        File mockDirectory2 = Mockito.mock(File.class);
        
        File[] files1 = new File[] { new File("dir1_file1.txt"), new File("dir1_file2.txt") };
        File[] files2 = new File[] { new File("dir2_file1.txt"), new File("dir2_file2.txt"), new File("dir2_file3.txt") };
        
        // Setup different return values for each directory
        when(mockDirectory1.listFiles()).thenReturn(files1);
        when(mockDirectory1.isDirectory()).thenReturn(true);
        when(mockDirectory1.getAbsolutePath()).thenReturn("/fake/path1");
        
        when(mockDirectory2.listFiles()).thenReturn(files2);
        when(mockDirectory2.isDirectory()).thenReturn(false);
        when(mockDirectory2.getAbsolutePath()).thenReturn("/fake/path2");
        
        // Create separate DetectableEnvironment instances
        DetectableEnvironment env1 = new DetectableEnvironment(mockDirectory1);
        DetectableEnvironment env2 = new DetectableEnvironment(mockDirectory2);
        
        File wrappedDir1 = env1.getDirectory();
        File wrappedDir2 = env2.getDirectory();
        
        // Call methods on both wrapped directories multiple times
        File[] result1a = wrappedDir1.listFiles();
        File[] result1b = wrappedDir1.listFiles();
        boolean isDir1a = wrappedDir1.isDirectory();
        boolean isDir1b = wrappedDir1.isDirectory();
        
        File[] result2a = wrappedDir2.listFiles();
        File[] result2b = wrappedDir2.listFiles();
        boolean isDir2a = wrappedDir2.isDirectory();
        boolean isDir2b = wrappedDir2.isDirectory();
        
        // Verify each underlying directory was called only once (caching works)
        verify(mockDirectory1, times(1)).listFiles();
        verify(mockDirectory1, times(1)).isDirectory();
        verify(mockDirectory2, times(1)).listFiles();
        verify(mockDirectory2, times(1)).isDirectory();
        
        // Verify that the caches return the correct results for each directory
        assertSame(result1a, result1b);
        assertArrayEquals(files1, result1a);
        assertTrue(isDir1a);
        assertTrue(isDir1b);
        
        assertSame(result2a, result2b);
        assertArrayEquals(files2, result2a);
        assertFalse(isDir2a);
        assertFalse(isDir2b);
        
        // Most importantly: verify that the results are different between directories
        assertFalse(result1a == result2a); // Different object references
        assertEquals(2, result1a.length);
        assertEquals(3, result2a.length);
        assertTrue(isDir1a);
        assertFalse(isDir2a);
    }
}