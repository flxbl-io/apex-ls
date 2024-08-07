/*
 * Copyright (c) 2022 FinancialForce.com, inc. All rights reserved.
 */

package io.github.apexdevtools.apexls.types;

import com.financialforce.types.IPropertyDeclaration;
import io.github.apexdevtools.apexls.api.ApexField;
import io.github.apexdevtools.apexls.api.ApexType;
import io.github.apexdevtools.apexls.api.ApexTypeId;
import scala.collection.immutable.ArraySeq;

public class ApexPropertyAdapter implements ApexField {
    final private ApexTypeAdapter owner;
    final private IPropertyDeclaration pd;

    public ApexPropertyAdapter(ApexTypeAdapter owner, IPropertyDeclaration pd) {
        this.owner = owner;
        this.pd = pd;
    }

    @Override
    public ApexType getOwner() {
        return owner;
    }

    @Override
    public String getFieldName() {
        return pd.id().toString();
    }

    @Override
    public String getModifiers() {
        return ArraySeq.unsafeWrapArray(pd.modifiers()).mkString(" ");
    }

    @Override
    public String getMemberType() {
        return "PROPERTY";
    }

    @Override
    public ApexTypeId getType() {
        return NameApexTypeId.apply(pd.typeRef());
    }
}
