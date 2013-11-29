#ifndef __JOYPLUS_PINCODELAYER_H__
#define __JOYPLUS_PINCODELAYER_H__

#include "cocos2d.h"
#include "cocos-ext.h"
#include "Log.h"
#include <jni.h>
#include "ui/CCImageView.h"
#include "platform/android/jni/Java_org_cocos2dx_lib_Cocos2dxHelper.h"
USING_NS_CC_EXT;
USING_NS_CC;
using namespace std;


class PincodeLayer : public CCLayer ,public CCImageViewDownLoadDelegte
{
public:
	virtual bool init();
	CREATE_FUNC(PincodeLayer);
	void setSelected(bool isSelected);
	void runBreath(CCTime dt);
	void setPincode(const char* pincode);
	void updateQQDisplay(const char* name, const char* url);
	CCSprite* createMaskedSprite(CCImageView* src, const char* maskFile);
	virtual void onResult(const char* url, bool isSucced);

private:
	CCImageView * imag_touxiang;
};

#endif