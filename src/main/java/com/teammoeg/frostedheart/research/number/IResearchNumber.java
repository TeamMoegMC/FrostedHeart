package com.teammoeg.frostedheart.research.number;

import com.teammoeg.frostedheart.research.data.ResearchData;

public interface IResearchNumber {
	double getVal(ResearchData rd);
	default int getInt(ResearchData rd) {
		return (int)getVal(rd);
	}
	default long getLong(ResearchData rd) {
		return (long)getVal(rd);
	}
}
