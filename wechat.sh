#!/bin/bash

#token file
token="wechat_accesstoken"

#message file
queue_file="wechat_msgqueue"

appid=
appsecret=
open_id=
template_id=

url="xxx.io"
name="Hey"

function getAccessToken {
    if [ -f "$token" ]; then
        access_token=`cat $token | awk -F":" '{print $1}'`
        expires_in=`cat $token | awk -F":" '{print $2}'`
        time=`cat $token | awk -F":" '{print $3}'`
            if [ -z $access_token ] || [ -z $expires_in ] || [ -z $time ]; then
            rm -f $token
            getAccessToken
        fi
    else
        content=$(curl "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=$appid&secret=$appsecret")
        echo "get content: $content"
        access_token=`echo $content | awk -F "\"" '{print $4}'`
        expires_in=`echo $content | awk -F "\"" '{print $7}' | cut -d"}" -f1 | cut -c2-`
        echo "access_token = $access_token"
        echo "expires_in = $expires_in"
        time=$(date +%s)
        echo "$access_token:$expires_in:$time" > $token

        if [ -z $access_token ] || [ -z $expires_in ] || [ -z $time ]; then
            echo "get access_token error"
            exit 0
        fi
    fi

    remain=$[$(date +%s) - $time]
    limit=$[$expires_in - 60]

    if [ $remain -gt $limit ]; then
        rm -f $token
        getAccessToken
    fi
}

function send {
    json=`cat << EOF
    {
    "touser":"$open_id",
    "template_id":"$template_id",
    "url":"$url",
    "data":{
            "name": {
                    "value":"$name",
                    "color":"#FF0000"
            },
            "date": {
                    "value":"$date",
                    "color":"#173177"
            },
            "message":{
                    "value":"$message",
                    "color":"#FF0000"
            }
        }
     }
EOF
`
   echo "send message: $json"
   curl -X POST -H "Content-Type: application/json" https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=$access_token -d "$json" 
}

while :
do
    if [[ -f $queue_file && `cat $queue_file` != "" ]]; then {
        message=`cat $queue_file`

        date=`date +%Y%m%d_%T`

        getAccessToken

        send

        > $queue_file
    }
    fi
    
    sleep 20
done