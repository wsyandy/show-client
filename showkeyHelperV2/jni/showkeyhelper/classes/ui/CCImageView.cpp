#include "CCImageView.h"

//CCImageView* CCImageView::createWithLocalPath(const char *path)
//{
//	CCImageView * image_view = new CCImageView();
//	if(image_view&&image_view->initWithFile(path))
//	{
//		image_view->autorelease();
//		return image_view;
//	}
//	CC_SAFE_DELETE(image_view);
//	return NULL;
//}
///**
// * url : http url
// *
// * default_loacl_path : default pic. local path
// *
// * boundsize : the size of CCImageView
// */
//CCImageView* CCImageView::createWithNetUrl(const char *url, const char *default_local_path, CCSize boundsize)
//{
//	return CCImageView::createWithNetUrl(url,default_local_path,boundsize,true,NULL);
//}

bool CCImageView::initWithUrl(const char *url, const char *defult)
{
	CCAssert(url != NULL, "Invalid url for sprite");

	setDefaultPic(defult);
//	string path = CCFileUtils::sharedFileUtils()->getWritablePath()+"test1.png";
	string path = CCFileUtils::sharedFileUtils()->getWritablePath()+ getFileNameFromUrl(url);
	if(initWithDownLoadFile(path.c_str()))
	{
		LOGD("CCImageView","initWithDownLoadFile success");
		return true;
	}else
	{
		LOGD("CCImageView","load from url --> %s", url);
		initWithFile(defult);
		CCHttpClient* httpClient = CCHttpClient::getInstance();
		CCHttpRequest* httpReq =new CCHttpRequest();
	//    设置请求类型
		httpReq->setRequestType(CCHttpRequest::kHttpGet);
	//  设置请求Url(可以更具需要从json 或xml,甚至html里解析获得到这个图片url)
//		httpReq->setUrl("http://avatar.csdn.net/A/6/5/1_qqxj2012.jpg");
		httpReq->setUrl(url);
//		    httpReq->setUrl("http://bs.baidu.com/netdisk/BaiduYunSetup_web_1T.apk");
	//    请求完成后回调
		httpReq->setResponseCallback(this,callfuncND_selector(CCImageView::onDownLoadComplete));
	//   为请求设置标签,后面可以根据这个标签来获取我们要的数据
		httpReq->setTag("PicGet");
	//    设置连接超时时间
		httpClient->setTimeoutForConnect(30);
		httpClient->send(httpReq);
		httpReq->release();
		httpReq=NULL;
		return true;
	}

}

void CCImageView::onDownLoadComplete(CCNode* node,CCObject* obj)
{
	CCHttpResponse* response = (CCHttpResponse*)obj;
//    判断是否响应成功
	const char* tag = response->getHttpRequest()->getTag();
	if (0 != strcmp("PicGet",tag))
	{
		return;
	}
	if (!response->isSucceed())
	{
		LOGD("onGetFinished","Receive Error! %s\n",response->getErrorBuffer());
		if(delegere){
			LOGD("onGetFinished","notice the delegete false");
			delegere->onResult(response->getHttpRequest()->getUrl(),false);
		}
		if(initWithFile(default_pic)){
		}else{
			LOGD("onDownLoadComplete","init with default pic %s",default_pic);
		}
		return ;
	}

	// 数据转存
	unsigned char* pBuffer = NULL;
	unsigned long bufferSize = 0;
	vector<char> *buffer = response->getResponseData();

	string buff(buffer->begin(),buffer->end());
	if(isSave){
		//保存到本地文件
		string path = CCFileUtils::sharedFileUtils()->getWritablePath()+ getFileNameFromUrl(response->getHttpRequest()->getUrl());
		pBuffer = CCFileUtils::sharedFileUtils()->getFileData(path.c_str(), "r", &bufferSize);
		LOGD("CCImageView","path: %s",path.c_str());
		FILE *fp = fopen(path.c_str(), "wb+");
		if(fp){
			fwrite(buff.c_str(), 1,buffer->size(),  fp);
			fclose(fp);
			if(!initWithDownLoadFile(path.c_str())){
				initWithFile(default_pic);
			}
			if(delegere){
				delegere->onResult(response->getHttpRequest()->getUrl(),true);
			}
		}else{
			LOGD("CCImageView","file：path: %s file is null",path.c_str());
			initWithFile(default_pic);
		}

	}else{
		CCImage* img = new CCImage;
		LOGD("CCImageView","img not save %d",buff.size());
		img->initWithImageData((char *)buff.c_str(),buff.size());
		free(pBuffer);
		CCTexture2D* texture = new cocos2d::CCTexture2D();
		bool isImg = texture->initWithImage(img);
		img->release();
		if(isImg){
			LOGD("CCImageView","--------is  img--------------");
			initWithTexture(texture);
			if(getContentSize().width!=0&&getContentSize().height!=0)
			{
				setScaleX(m_size.width/getContentSize().width);
				setScaleY(m_size.height/getContentSize().height);
			}
			texture->release();
			if(delegere){
				delegere->onResult(response->getHttpRequest()->getUrl(),true);
			}
		}else{
			initWithFile(default_pic);
			LOGD("CCImageView","--------not  img--------------");
		}
	}
}

CCImageView* CCImageView::createWithNetUrl(const char* url,
		const char* default_local_path, CCSize boundsize, bool isSave, CCImageViewDownLoadDelegte * delegte)
{
	CCImageView * image_view = new CCImageView();
	if(image_view){
		image_view->autorelease();
		image_view->isSave = isSave;
		image_view->setDefaultPic(default_local_path);
		if(delegte){
			image_view->delegere = delegte;
		}
		image_view->setBoundSize(boundsize);
		image_view->initWithUrl(url,default_local_path);
		LOGD("CCImageView","create success");
		return image_view;
	}
	CC_SAFE_DELETE(image_view);
	return NULL;
}

bool CCImageView::initWithUrl(const char* url, const char* default_local_path,
		bool isSave) {
	this->isSave = isSave;
	return initWithUrl(url,default_local_path);
}

bool CCImageView::initWithDownLoadFile(const char * filePath)
{
	unsigned char* pBuffer = 0;
	unsigned long bufferSize = 0;
	pBuffer = CCFileUtils::sharedFileUtils()->getFileData(filePath, "r", &bufferSize);
	LOGD("CCImageView","size = %lu", bufferSize);
	if(bufferSize!=0){

		CCImage* img = new CCImage;
		img->initWithImageData(pBuffer,bufferSize);
		free(pBuffer);
		CCTexture2D* texture = new cocos2d::CCTexture2D();
		bool isImg = texture->initWithImage(img);
		img->release();
		if(!isImg){
			LOGD("CCImageView","%s is not image" ,filePath);
			return false;
		}
		initWithTexture(texture);
		if(getContentSize().width!=0&&getContentSize().height!=0)
		{
			setScaleX(m_size.width/getContentSize().width);
			setScaleY(m_size.height/getContentSize().height);
		}
		texture->release();
	}else{
		LOGD("CCImageView","%s is not exists" ,filePath);
		return false;
	}

}

CCSize CCImageView::getBoundSize()
{
	return m_size;
}

void CCImageView::setBoundSize(CCSize size)
{
	m_size = size;
	if(getContentSize().width!=0&&getContentSize().height!=0)
	{
		setScaleX(m_size.width/getContentSize().width);
		setScaleY(m_size.height/getContentSize().height);
	}
}
/**
 * get file name from url
 * example:
 * 		url = "http://avatar.csdn.net/A/6/5/1_qqxj2012.jpg"
 * 		return "1_qqxj2012"
 */
string CCImageView::getFileNameFromUrl(const char * url)
{
	string str(url);
	string pattern1 = "/";
	string pattern2 = ".";
	LOGD("CCImageView", "url ----> %s", str.c_str());
	if(str.find("/")!=string::npos)
	{
		vector<string> str_list = stringSplit(str,pattern1);
		str = str_list[str_list.size()-1];
	}

	if(str.find(".")!=string::npos)
	{
		vector<string> str_list_1 = stringSplit(str,pattern2);
		str = str_list_1[0];
	}

//	replace(str.begin(),str.end(),'/','_');
//	replace(str.begin(),str.end()-5,'.','_');

	LOGD("CCImageView", "file name ----> %s", str.c_str());
	return str;
}

