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
    
    // @Test
    // public void testDirectoryCacheMemoizesListFiles() {
    //     // Create a mock directory to track method calls
    //     File mockDirectory = Mockito.mock(File.class);
    //     File[] expectedFiles = new File[] { new File("file1.txt"), new File("file2.txt") };
        
    //     // Mock the listFiles method to return our expected files
    //     when(mockDirectory.listFiles()).thenReturn(expectedFiles);
    //     when(mockDirectory.getAbsolutePath()).thenReturn("/fake/path");
        
    //     // Test the DirectoryCache directly since it's package-visible
    //     DetectableEnvironment.DirectoryCache cache = new DetectableEnvironment.DirectoryCache(mockDirectory);
        
    //     // Call listFiles multiple times
    //     File[] files1 = cache.listFiles();
    //     File[] files2 = cache.listFiles();
    //     File[] files3 = cache.listFiles();
        
    //     // Verify the underlying listFiles was called only once
    //     verify(mockDirectory, times(1)).listFiles();
        
    //     // Verify all calls return the same cached result
    //     assertSame(files1, files2);
    //     assertSame(files2, files3);
    //     assertArrayEquals(expectedFiles, files1);
    // }
    
    // @Test
    // public void testDirectoryCacheMemoizesIsDirectoryTrue() {
    //     // Create a mock directory to track method calls
    //     File mockDirectory = Mockito.mock(File.class);
        
    //     // Mock the isDirectory method to return true
    //     when(mockDirectory.isDirectory()).thenReturn(true);
    //     when(mockDirectory.getAbsolutePath()).thenReturn("/fake/path");
        
    //     // Test the DirectoryCache directly
    //     DetectableEnvironment.DirectoryCache cache = new DetectableEnvironment.DirectoryCache(mockDirectory);
        
    //     // Call isDirectory multiple times
    //     boolean isDir1 = cache.isDirectory();
    //     boolean isDir2 = cache.isDirectory();
    //     boolean isDir3 = cache.isDirectory();
        
    //     // Verify the underlying isDirectory was called only once
    //     verify(mockDirectory, times(1)).isDirectory();
        
    //     // Verify all calls return the same cached result
    //     assertTrue(isDir1);
    //     assertTrue(isDir2);
    //     assertTrue(isDir3);
    // }
    
    // @Test
    // public void testDirectoryCacheMemoizesIsDirectoryFalse() {
    //     // Create a mock directory to track method calls
    //     File mockDirectory = Mockito.mock(File.class);
        
    //     // Mock the isDirectory method to return false
    //     when(mockDirectory.isDirectory()).thenReturn(false);
    //     when(mockDirectory.getAbsolutePath()).thenReturn("/fake/path");
        
    //     // Test the DirectoryCache directly
    //     DetectableEnvironment.DirectoryCache cache = new DetectableEnvironment.DirectoryCache(mockDirectory);
        
    //     // Call isDirectory multiple times
    //     boolean isDir1 = cache.isDirectory();
    //     boolean isDir2 = cache.isDirectory();
    //     boolean isDir3 = cache.isDirectory();
        
    //     // Verify the underlying isDirectory was called only once
    //     verify(mockDirectory, times(1)).isDirectory();
        
    //     // Verify all calls return the same cached result
    //     assertFalse(isDir1);
    //     assertFalse(isDir2);
    //     assertFalse(isDir3);
    // }
    
    // @Test
    // public void testDirectoryCacheHandlesNullListFiles() {
    //     // Create a mock directory to track method calls
    //     File mockDirectory = Mockito.mock(File.class);
        
    //     // Mock the listFiles method to return null (e.g., not a directory or IO error)
    //     when(mockDirectory.listFiles()).thenReturn(null);
    //     when(mockDirectory.getAbsolutePath()).thenReturn("/fake/path");
        
    //     // Test the DirectoryCache directly
    //     DetectableEnvironment.DirectoryCache cache = new DetectableEnvironment.DirectoryCache(mockDirectory);
        
    //     // Call listFiles multiple times
    //     File[] files1 = cache.listFiles();
    //     File[] files2 = cache.listFiles();
        
    //     // Verify the underlying listFiles was called only once
    //     verify(mockDirectory, times(1)).listFiles();
        
    //     // Verify all calls return the same cached result (null)
    //     assertNull(files1);
    //     assertNull(files2);
    // }
    
    // @Test
    // public void testWrappedDirectoryDelegatestoCache() {
    //     // Create a mock directory
    //     File mockDirectory = Mockito.mock(File.class);
    //     File[] expectedFiles = new File[] { new File("file1.txt") };
        
    //     when(mockDirectory.listFiles()).thenReturn(expectedFiles);
    //     when(mockDirectory.isDirectory()).thenReturn(true);
    //     when(mockDirectory.getAbsolutePath()).thenReturn("/fake/path");
        
    //     DetectableEnvironment environment = new DetectableEnvironment(mockDirectory);
    //     File wrappedDirectory = environment.getDirectory();
        
    //     // Call methods on the wrapped directory
    //     File[] files1 = wrappedDirectory.listFiles();
    //     File[] files2 = wrappedDirectory.listFiles();
    //     boolean isDir1 = wrappedDirectory.isDirectory();
    //     boolean isDir2 = wrappedDirectory.isDirectory();
        
    //     // Verify memoization works through the wrapper
    //     verify(mockDirectory, times(1)).listFiles();
    //     verify(mockDirectory, times(1)).isDirectory();
        
    //     assertSame(files1, files2);
    //     assertEquals(isDir1, isDir2);
    //     assertTrue(isDir1);
    // }
}