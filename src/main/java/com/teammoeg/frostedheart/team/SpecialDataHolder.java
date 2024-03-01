package com.teammoeg.frostedheart.team;

import java.util.Optional;

import com.teammoeg.frostedheart.util.io.NBTSerializable;

public interface SpecialDataHolder<U extends SpecialDataHolder<U>> {
	<T extends NBTSerializable> T getData(SpecialDataType<T,U> cap);
	<T extends NBTSerializable> Optional<T> getOptional(SpecialDataType<T,U> cap);
}
