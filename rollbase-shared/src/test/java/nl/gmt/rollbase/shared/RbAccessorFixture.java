package nl.gmt.rollbase.shared;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class RbAccessorFixture {
    @Test
    public void referenceAccessor() throws  RollbaseException {
        MyBean bean = new MyBean();

        RbAccessor accessor = createAccessor("String");
        accessor.setValue(bean, "Hello world!");
        assertEquals("Hello world!", accessor.getValue(bean));
    }

    @Test
    public void primitiveAccessor() throws  RollbaseException {
        MyBean bean = new MyBean();

        RbAccessor accessor = createAccessor("Int");
        accessor.setValue(bean, 7);
        assertEquals(7, accessor.getValue(bean));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void notSettableAccessor() throws RollbaseException {
        MyBean bean = new MyBean();

        RbAccessor accessor = createAccessor("List");
        ((List<String>)accessor.getValue(bean)).add("Hello world!");
        assertFalse(accessor.isSettable());

        try {
            accessor.setValue(bean, null);
            fail();
        } catch (IllegalArgumentException e) {
            // Expect an exception.
        }
    }

    private RbAccessor createAccessor(String name) throws RollbaseException {
        Method getter = null;
        Method setter = null;

        for (Method method : MyBean.class.getMethods()) {
            if (method.getName().equals("get" + name) || method.getName().equals("is" + name)) {
                getter = method;
            } else if (method.getName().equals("set" + name)) {
                setter = method;
            }
        }

        return RbAccessor.createAccessor(getter, setter);
    }

    public static class MyBean {
        private String string;
        private List<String> list;
        private int intValue;

        public String getString() {
            return string;
        }

        public void setString(String string) {
            this.string = string;
        }

        public List<String> getList() {
            if (list == null) {
                list = new ArrayList<>();
            }
            return list;
        }

        public int getInt() {
            return intValue;
        }

        public void setInt(int intValue) {
            this.intValue = intValue;
        }
    }
}
