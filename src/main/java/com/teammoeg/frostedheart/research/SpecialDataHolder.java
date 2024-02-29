package com.teammoeg.frostedheart.research;

import com.teammoeg.frostedheart.util.NBTSerializable;

public interface SpecialDataHolder<U extends SpecialDataHolder<U>> {
	<T extends NBTSerializable> T getData(SpecialDataType<T,U> cap);
}
