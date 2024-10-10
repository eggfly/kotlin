/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.psi.stubs.elements;

import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.constant.ConstantValue;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtProperty;
import org.jetbrains.kotlin.psi.psiUtil.KtPsiUtilKt;
import org.jetbrains.kotlin.psi.stubs.KotlinPropertyStub;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinConstantValueKt;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPropertyStubImpl;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinStubOrigin;

import java.io.IOException;

public class KtPropertyElementType extends KtStubElementType<KotlinPropertyStub, KtProperty> {
    public KtPropertyElementType(@NotNull @NonNls String debugName) {
        super(debugName, KtProperty.class, KotlinPropertyStub.class);
    }

    @NotNull
    @Override
    public KotlinPropertyStub createStub(@NotNull KtProperty psi, StubElement parentStub) {
        assert !psi.isLocal() :
                String.format("Should not store local property: %s, parent %s",
                              psi.getText(), psi.getParent() != null ? psi.getParent().getText() : "<no parent>");

        return new KotlinPropertyStubImpl(
                (StubElement<?>) parentStub, StringRef.fromString(psi.getName()),
                psi.isVar(), psi.isTopLevel(), psi.hasInitializer(),
                psi.getReceiverTypeReference() != null, psi.getTypeReference() != null,
                KtPsiUtilKt.safeFqNameForLazyResolve(psi),
                /* constantInitializer = */ null,
                /* origin = */ null
        );
    }

    @Override
    public void serialize(@NotNull KotlinPropertyStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getName());
        dataStream.writeBoolean(stub.isVar());
        dataStream.writeBoolean(stub.isTopLevel());
        dataStream.writeBoolean(stub.hasInitializer());
        dataStream.writeBoolean(stub.isExtension());
        dataStream.writeBoolean(stub.hasReturnTypeRef());

        FqName fqName = stub.getFqName();
        dataStream.writeName(fqName != null ? fqName.asString() : null);

        if (stub instanceof KotlinPropertyStubImpl) {
            KotlinPropertyStubImpl stubImpl = (KotlinPropertyStubImpl) stub;

            ConstantValue<?> constantInitializer = ((KotlinPropertyStubImpl) stub).getConstantInitializer();
            if (constantInitializer != null) {
                KotlinConstantValueKt.serialize(constantInitializer, dataStream);
            } else {
                dataStream.writeInt(-1);
            }

            KotlinStubOrigin.serialize(stubImpl.getOrigin(), dataStream);
        }
    }

    @NotNull
    @Override
    public KotlinPropertyStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        StringRef name = dataStream.readName();
        boolean isVar = dataStream.readBoolean();
        boolean isTopLevel = dataStream.readBoolean();
        boolean hasInitializer = dataStream.readBoolean();
        boolean hasReceiverTypeRef = dataStream.readBoolean();
        boolean hasReturnTypeRef = dataStream.readBoolean();

        StringRef fqNameAsString = dataStream.readName();
        FqName fqName = fqNameAsString != null ? new FqName(fqNameAsString.toString()) : null;

        return new KotlinPropertyStubImpl(
                (StubElement<?>) parentStub, name, isVar, isTopLevel, hasInitializer,
                hasReceiverTypeRef, hasReturnTypeRef, fqName, KotlinConstantValueKt.createConstantValue(dataStream),
                KotlinStubOrigin.deserialize(dataStream)
        );
    }

    @Override
    public void indexStub(@NotNull KotlinPropertyStub stub, @NotNull IndexSink sink) {
        StubIndexService.getInstance().indexProperty(stub, sink);
    }
}
