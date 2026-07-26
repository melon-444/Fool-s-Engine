package com.melon.foolsEngine.util;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.Method;

import java.util.Objects;

public final class ConstructorInjector {

    private static final int ASM_API = Opcodes.ASM9;

    private static final String SUBSCRIBER_ANNOTATION =
            "Lcom/melon/foolsEngine/core/annotation/InstanceBusSubscriber;";

    private static final Type EVENT_BUS_TYPE =
            Type.getObjectType(
                    "com/melon/foolsEngine/core/events/EventBus"
            );

    private static final Method ADD_LISTENER_METHOD =
            new Method(
                    "addListener",
                    "(Ljava/lang/Object;)V"
            );

    private ConstructorInjector() {
    }

    public static byte[] inject(byte[] classBytes) {
        Objects.requireNonNull(classBytes, "classBytes");

        final ClassReader reader;

        try {
            reader = new ClassReader(classBytes);
        } catch (IllegalArgumentException exception) {
            // 不是 ASM 支持的有效 class 文件
            return classBytes;
        }

        /*
         * 第一次读取：只检查类本身是否有目标注解。
         *
         * 不能只搜索常量池字符串，因为类可能只是引用了这个注解，
         * 注解也可能出现在字段或方法上。
         */
        AnnotationDetector detector = new AnnotationDetector();

        reader.accept(
                detector,
                ClassReader.SKIP_CODE
                        | ClassReader.SKIP_DEBUG
                        | ClassReader.SKIP_FRAMES
        );

        if (!detector.hasSubscriberAnnotation()) {
            return classBytes;
        }

        /*
         * 这里只插入一段栈平衡代码：
         *
         *   aload_0
         *   invokestatic EventBus.addListener(Object)
         *
         * 不增加控制流分支，所以保留原 StackMap Frame 即可，
         * 只让 ASM 重新计算 maxStack/maxLocals。
         *
         * 这样还能避免 COMPUTE_FRAMES 在自定义 ClassLoader 环境下
         * 调用 getCommonSuperClass() 时发生类加载问题。
         */
        ClassWriter writer = new ClassWriter(
                reader,
                ClassWriter.COMPUTE_MAXS
        );

        ClassVisitor injector = new ClassVisitor(ASM_API, writer) {

            @Override
            public MethodVisitor visitMethod(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions
            ) {
                MethodVisitor visitor = super.visitMethod(
                        access,
                        name,
                        descriptor,
                        signature,
                        exceptions
                );

                if (!"<init>".equals(name)) {
                    return visitor;
                }

                return new AdviceAdapter(
                        ASM_API,
                        visitor,
                        access,
                        name,
                        descriptor
                ) {
                    @Override
                    protected void onMethodEnter() {
                        /*
                         * 对普通方法，onMethodEnter() 位于方法开头。
                         *
                         * 对构造器，AdviceAdapter 会跟踪 uninitializedThis，
                         * 直到 super(...) 或 this(...) 调用完成后才执行这里，
                         * 因而此时 aload_0 可以安全传递给普通静态方法。
                         */
                        loadThis();
                        invokeStatic(
                                EVENT_BUS_TYPE,
                                ADD_LISTENER_METHOD
                        );
                    }
                };
            }
        };

        /*
         * AdviceAdapter 需要展开的 Frame 来正确跟踪构造器中的
         * uninitializedThis 状态。
         */
        reader.accept(injector, ClassReader.EXPAND_FRAMES);

        return writer.toByteArray();
    }

    private static final class AnnotationDetector extends ClassVisitor {

        private boolean subscriberAnnotation;

        private AnnotationDetector() {
            super(ASM_API);
        }

        @Override
        public AnnotationVisitor visitAnnotation(
                String descriptor,
                boolean visible
        ) {
            /*
             * @Retention(RUNTIME) 对应 visible == true。
             * 同时这只检查 class_info 的类级注解，不会误判方法和字段。
             */
            if (visible && SUBSCRIBER_ANNOTATION.equals(descriptor)) {
                subscriberAnnotation = true;
            }

            return null;
        }

        private boolean hasSubscriberAnnotation() {
            return subscriberAnnotation;
        }
    }
}