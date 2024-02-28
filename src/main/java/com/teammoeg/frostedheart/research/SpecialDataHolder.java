package com.teammoeg.frostedheart.research;

import com.teammoeg.frostedheart.util.NBTSerializable;

public interface SpecialDataHolder {
	<T extends NBTSerializable> T getData(SpecialDataType<T> cap);
}
