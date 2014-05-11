package nl.gmt.rollbase.shared;

import nl.gmt.rollbase.shared.schema.RbId;
import nl.gmt.rollbase.shared.schema.RbIdType;
import org.apache.commons.lang.Validate;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.UUID;

public abstract class RbAccessor {
    static RbAccessor createAccessor(Method getter, Method setter) throws RollbaseException {
        return Builder.build(getter, setter);
    }

    private final Class<?> type;
    private final boolean settable;
    private final RbIdType idType;
    private final String name;

    protected RbAccessor(String name, Class<?> type, boolean settable, RbIdType idType) {
        Validate.notNull(name, "name");
        Validate.notNull(type, "type");
        Validate.notNull(idType, "idType");

        this.name = name;
        this.type = type;
        this.settable = settable;
        this.idType = idType;
    }

    public boolean isSettable() {
        return settable;
    }

    public RbIdType getIdType() {
        return idType;
    }

    public String getName() {
        return name;
    }

    public Class<?> getType() {
        return type;
    }

    @Override
    public String toString() {
        return name;
    }

    public abstract Object getValue(Object object);

    public abstract void setValue(Object object, Object value);

    private static class Builder implements Opcodes {
        private static final MyClassLoader CLASS_LOADER = new MyClassLoader(RbAccessor.class.getClassLoader());
        private static final Constructor<?> CONSTRUCTOR;
        private static final Constructor<?> IAE_CONSTRUCTOR;
        private static final Method GET_VALUE;
        private static final Method SET_VALUE;

        static {
            try {
                CONSTRUCTOR = RbAccessor.class.getDeclaredConstructor(String.class, Class.class, boolean.class, RbIdType.class);
                CONSTRUCTOR.setAccessible(true);

                IAE_CONSTRUCTOR = IllegalArgumentException.class.getDeclaredConstructor(String.class);

                Method getValue = null;
                Method setValue = null;

                for (Method method : RbAccessor.class.getMethods()) {
                    switch (method.getName()) {
                        case "getValue": getValue = method; break;
                        case "setValue": setValue = method; break;
                    }
                }

                GET_VALUE = getValue;
                SET_VALUE = setValue;
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        public static RbAccessor build(Method getter, Method setter) throws RollbaseException {
            String className = "Accessor_" + UUID.randomUUID().toString().replace('-', '_');
            String accessorName = getter.getName().startsWith("get")
                ? getter.getName().substring(3)
                : getter.getName().substring(2);
            String accessorClassName = Type.getInternalName(RbAccessor.class);
            String declaringClassName = Type.getInternalName(getter.getDeclaringClass());
            Class<?> type = getter.getReturnType();
            Class<?> boxedType = getBoxedType(type);

            try {
                ClassWriter cw = new ClassWriter(0);
                MethodVisitor mv;

                cw.visit(V1_7, ACC_PUBLIC + ACC_SUPER, className, null, accessorClassName, null);

                {
                    mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
                    mv.visitCode();
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitLdcInsn(accessorName);
                    mv.visitLdcInsn(Type.getType(boxedType));
                    mv.visitInsn(setter == null ? ICONST_0 : ICONST_1);

                    RbId annotation = getter.getAnnotation(RbId.class);
                    RbIdType idType = annotation == null ? RbIdType.NONE : annotation.type();

                    mv.visitFieldInsn(GETSTATIC, Type.getInternalName(RbIdType.class), idType.toString(), Type.getDescriptor(RbIdType.class));
                    mv.visitMethodInsn(INVOKESPECIAL, accessorClassName, "<init>", Type.getConstructorDescriptor(CONSTRUCTOR));
                    mv.visitInsn(RETURN);
                    mv.visitMaxs(5, 1);
                    mv.visitEnd();
                }
                {
                    mv = cw.visitMethod(ACC_PUBLIC, GET_VALUE.getName(), Type.getMethodDescriptor(GET_VALUE), null, null);

                    mv.visitCode();
                    mv.visitVarInsn(ALOAD, 1);
                    mv.visitTypeInsn(CHECKCAST, declaringClassName);
                    mv.visitMethodInsn(INVOKEVIRTUAL, declaringClassName, getter.getName(), Type.getMethodDescriptor(getter));

                    if (type.isPrimitive()) {
                        Method boxMethod = boxedType.getMethod("valueOf", type);
                        mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(boxedType), boxMethod.getName(), Type.getMethodDescriptor(boxMethod));
                    }

                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(2, 2);
                    mv.visitEnd();
                }
                {
                    mv = cw.visitMethod(ACC_PUBLIC, SET_VALUE.getName(), Type.getMethodDescriptor(SET_VALUE), null, null);
                    mv.visitCode();

                    if (setter == null) {
                        mv.visitTypeInsn(NEW, Type.getInternalName(IllegalArgumentException.class));
                        mv.visitInsn(DUP);
                        mv.visitLdcInsn("Cannot set value");
                        mv.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(IllegalArgumentException.class), "<init>", Type.getConstructorDescriptor(IAE_CONSTRUCTOR));
                        mv.visitInsn(ATHROW);
                        mv.visitMaxs(3, 3);
                    } else {
                        mv.visitVarInsn(ALOAD, 1);
                        mv.visitTypeInsn(CHECKCAST, declaringClassName);
                        mv.visitVarInsn(ALOAD, 2);

                        if (type.isPrimitive()) {
                            mv.visitTypeInsn(CHECKCAST, Type.getInternalName(getBoxedType(setter.getParameterTypes()[0])));
                            Method unboxMethod = boxedType.getMethod(getMethodName(type));
                            mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(boxedType), unboxMethod.getName(), Type.getMethodDescriptor(unboxMethod));
                        } else {
                            mv.visitTypeInsn(CHECKCAST, Type.getInternalName(setter.getParameterTypes()[0]));
                        }

                        mv.visitMethodInsn(INVOKEVIRTUAL, declaringClassName, setter.getName(), Type.getMethodDescriptor(setter));
                        mv.visitInsn(RETURN);
                        mv.visitMaxs(3, 3);
                    }
                    mv.visitEnd();
                }

                cw.visitEnd();

                Class<?> klass = CLASS_LOADER.defineClass(className, cw.toByteArray());

                return (RbAccessor)klass.newInstance();
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException e) {
                throw new RollbaseException("Cannot create accessor", e);
            }
        }

        private static Class<?> getBoxedType(Class<?> type) {
            switch (Type.getType(type).getSort()) {
                case Type.BYTE: return Byte.class;
                case Type.BOOLEAN: return Boolean.class;
                case Type.SHORT: return Short.class;
                case Type.CHAR: return Character.class;
                case Type.INT: return Integer.class;
                case Type.FLOAT: return Float.class;
                case Type.LONG: return Long.class;
                case Type.DOUBLE: return Double.class;
                default: return type;
            }
        }

        private static String getMethodName(Class<?> type) {
            switch (Type.getType(type).getSort()) {
                case Type.BYTE: return "byteValue";
                case Type.BOOLEAN: return "booleanValue";
                case Type.SHORT: return "shortValue";
                case Type.CHAR: return "charValue";
                case Type.INT: return "intValue";
                case Type.FLOAT: return "floatValue";
                case Type.LONG: return "longValue";
                case Type.DOUBLE: return "doubleValue";
                default: return null;
            }
        }

        private static class MyClassLoader extends ClassLoader {
            private MyClassLoader(ClassLoader parent) {
                super(parent);
            }

            public Class<?> defineClass(String name, byte[] bytes) {
                return defineClass(name, bytes, 0, bytes.length);
            }
        }
    }
}
