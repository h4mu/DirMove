package com.dirmove;

import org.junit.Test;
import static org.junit.Assert.*;

public class DirectoryModelTest {
    @Test
    public void testDirectoryModel() {
        DirectoryModel model = new DirectoryModel("test", "/path/test", true);
        assertEquals("test", model.getName());
        assertEquals("/path/test", model.getPath());
        assertTrue(model.isSelected());

        model.setSelected(false);
        assertFalse(model.isSelected());
    }
}
