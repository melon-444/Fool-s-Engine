// foolsEngine - A custom 3D game engine in Java
// Copyright (C) 2026  melon_444
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
            // Not a valid class file that ASM supported
            return classBytes;
        }

        /*
         * First read: only check if target class has target annotation
         *
         * Not just search the constant pool，cause the class might only referenced the annotation.
         * The annotation might also appear at methods or fields.
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
         * Insert a stack balanced code：
         *
         *   aload_0
         *   invokestatic EventBus.addListener(Object)
         *
         * Do not add control branch, so we can simply keep the original StackMap Frame,
         * make ASM recalculate maxStack/maxLocals。
         *
         * This also avoid COMPUTE_FRAMES at custom ClassLoader environment
         * occurs class loading exception when invoke getCommonSuperClass().
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
                         * As for common methods，onMethodEnter() at the beginning of method itself.
                         *
                         * As for constructor，AdviceAdapter will track uninitializedThis，
                         * until super(...) or this(...) invoked so that here will be executed，
                         * So at the  aload_0 can safely deliver to static methods.
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
         * AdviceAdapter 需要展开的 Frame to correctly track the
         * uninitializedThis state in the constructor.
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
             * @Retention(RUNTIME) refers to visible == true。
             * Also this only check class level annotation of class_info,
             * will not misjudge methods and fields.
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