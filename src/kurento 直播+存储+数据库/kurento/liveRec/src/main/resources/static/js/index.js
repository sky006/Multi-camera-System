/**
 * rtsp://admin:a12345678@192.168.1.200/mpeg4/ch1/sub/av_stream
 */

var ws = new WebSocket('wss://' + location.host + '/recording');
var video1,video2,video3,video4/*,video5,video6*/;
var webRtcPeer1,webRtcPeer2,webRtcPeer3,webRtcPeer4/*,webRtcPeer5,webRtcPeer6*/;
var state;
var url1="rtsp://admin:a12345678@192.168.1.10:554/mpeg4/ch1/sub/av_stream";
var url2="rtsp://admin:a12345678@192.168.1.161:554/mpeg4/ch1/sub/av_stream";
var url3="rtsp://admin:a12345678@192.168.1.150:554/mpeg4/ch1/sub/av_stream";
var url4="rtsp://admin:a12345678@192.168.1.19:554/mpeg4/ch1/sub/av_stream";
//var url5="rtsp://admin:a12345678@192.168.1.10/mpeg4/ch1/sub/av_stream";
//var url6="rtsp://admin:a12345678@192.168.1.10/mpeg4/ch1/sub/av_stream";
var rec1="file:///home/iris/CameraRec/Camera1/";
var rec2="file:///home/iris/CameraRec/Camera2/";
var rec3="file:///home/iris/CameraRec/Camera3/";
var rec4="file:///home/iris/CameraRec/Camera4/";
//var rec5="file:///home/iris/CameraRec/Camera5/";
//var rec6="file:///home/iris/CameraRec/Camera6/";

const NO_CALL = 0;
const IN_CALL = 1;
const DISABLED = 2;

//加载页面时
window.onload = function() {
	//控制台信息
	console = new Console();
	video1 = document.getElementById('video1');
	video2 = document.getElementById('video2');
	video3 = document.getElementById('video3');
	video4 = document.getElementById('video4');
	//video5 = document.getElementById('video5');
	//video6 = document.getElementById('video6');
	setState(NO_CALL);
	//start();
}
//关闭页面前
window.onbeforeunload = function() {
	stop();
	ws.close();
}

//设置状态
function setState(nextState) {
	switch (nextState) {
	case NO_CALL:
		$('#start').attr('disabled', false);	//无状态中可以开始
		$('#stop').attr('disabled', true);
		break;
	case DISABLED:
		$('#start').attr('disabled', true);
		$('#stop').attr('disabled', true);
		break;
	case IN_CALL:
		$('#start').attr('disabled', true);
		$('#stop').attr('disabled', false);	//视频可以停止
		break;	
	default:
		onError('Unknown state ' + nextState);
	return;
	}
	state = nextState;
}
//监听
ws.onmessage = function(message) {
	var parsedMessage = JSON.parse(message.data);
	console.info('Received message: ' + message.data);

	switch (parsedMessage.id) {
	case 'startResponse':
		startResponse(parsedMessage);
		break;
	case 'playResponse':
		playResponse(parsedMessage);
		break;
	case 'playEnd':
		playEnd();
		break;
	case 'error':
		setState(NO_CALL);
		onError('Error message from server: ' + parsedMessage.message);
		break;
	case 'iceCandidate':
		switch (parsedMessage.num) {
		case '1':
			webRtcPeer1.addIceCandidate(parsedMessage.candidate, function(error) {
				if (error)
					return console.error('Error adding candidate: ' + error);
			});
			break;
		case '2':
			webRtcPeer2.addIceCandidate(parsedMessage.candidate, function(error) {
				if (error)
					return console.error('Error adding candidate: ' + error);
			});
			break;
		case '3':
			webRtcPeer3.addIceCandidate(parsedMessage.candidate, function(error) {
				if (error)
					return console.error('Error adding candidate: ' + error);
			});
			break;
		case '4':
			webRtcPeer4.addIceCandidate(parsedMessage.candidate, function(error) {
				if (error)
					return console.error('Error adding candidate: ' + error);
			});
			break;
		/*case '5':
			webRtcPeer5.addIceCandidate(parsedMessage.candidate, function(error) {
				if (error)
					return console.error('Error adding candidate: ' + error);
			});
			break;
		case '6':
			webRtcPeer6.addIceCandidate(parsedMessage.candidate, function(error) {
				if (error)
					return console.error('Error adding candidate: ' + error);
			});
			break;*/
		}
		break;
	case 'stopped':
		break;
	case 'paused':
		break;
	case 'recording':
		break;
	default:
		setState(NO_CALL);
	onError('Unrecognized message', parsedMessage);
	}
}

function start() {
	console.log('Starting video call ...');

	// Disable start button
	setState(DISABLED);
	showSpinner(video1,video2,video3,video4/*,video5,video6*/);
	console.log('Creating WebRtcPeer and generating local sdp offer ...');
	
	var option1 = {
			remoteVideo : video1,
			onicecandidate : onIceCandidate1
	}
	
	var option2 = {
			remoteVideo : video2,
			onicecandidate : onIceCandidate2
	}

	var option3 = {
			remoteVideo : video3,
			onicecandidate : onIceCandidate3
	}

	var option4 = {
			remoteVideo : video4,
			onicecandidate : onIceCandidate4
	}
	
	/*var option5 = {
			remoteVideo : video5,
			onicecandidate : onIceCandidate5
	}
	
	var option6 = {
			remoteVideo : video6,
			onicecandidate : onIceCandidate6
	}*/
	
	webRtcPeer1 = new kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly(option1,
			function(error) {
		if (error)
			return console.error(error);
		webRtcPeer1.generateOffer(onOffer1);
	});
	
	webRtcPeer2 = new kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly(option2,
			function(error) {
		if (error)
			return console.error(error);
		webRtcPeer2.generateOffer(onOffer2);
	});
	
	webRtcPeer3 = new kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly(option3,
			function(error) {
		if (error)
			return console.error(error);
		webRtcPeer3.generateOffer(onOffer3);
	});
	
	webRtcPeer4 = new kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly(option4,
			function(error) {
		if (error)
			return console.error(error);
		webRtcPeer4.generateOffer(onOffer4);
	});

	/*webRtcPeer5 = new kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly(option5,
			function(error) {
		if (error)
			return console.error(error);
		webRtcPeer5.generateOffer(onOffer5);
	});
	
	webRtcPeer6 = new kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly(option6,
			function(error) {
		if (error)
			return console.error(error);
		webRtcPeer6.generateOffer(onOffer6);
	});*/
}

function onOffer1(error, offerSdp) {
	if (error)
		return console.error('Error generating the offer');
	console.info('Invoking SDP offer callback function ' + location.host);
	var message = {
			id : 'start',
			num : '1',
			videourl : url1,
			recpath : rec1,
			sdpOffer : offerSdp
	}
	sendMessage(message);
}

function onOffer2(error, offerSdp) {
	if (error)
		return console.error('Error generating the offer');
	console.info('Invoking SDP offer callback function ' + location.host);
	var message = {
			id : 'start',
			num : '2',
			videourl : url2,
			recpath : rec2,
			sdpOffer : offerSdp
	}
	sendMessage(message);
}

function onOffer3(error, offerSdp) {
	if (error)
		return console.error('Error generating the offer');
	console.info('Invoking SDP offer callback function ' + location.host);
	var message = {
			id : 'start',
			num : '3',
			videourl : url3,
			recpath : rec3,
			sdpOffer : offerSdp
	}
	sendMessage(message);
}

function onOffer4(error, offerSdp) {
	if (error)
		return console.error('Error generating the offer');
	console.info('Invoking SDP offer callback function ' + location.host);
	var message = {
			id : 'start',
			num : '4',
			videourl : url4,
			recpath : rec4,
			sdpOffer : offerSdp
	}
	sendMessage(message);
}

/*function onOffer5(error, offerSdp) {
	if (error)
		return console.error('Error generating the offer');
	console.info('Invoking SDP offer callback function ' + location.host);
	var message = {
			id : 'start',
			num : '5',
			videourl : url5,
			recpath : rec5,
			sdpOffer : offerSdp
	}
	sendMessage(message);
}

function onOffer6(error, offerSdp) {
	if (error)
		return console.error('Error generating the offer');
	console.info('Invoking SDP offer callback function ' + location.host);
	var message = {
			id : 'start',
			num : '6',
			videourl : url6,
			recpath : rec6,
			sdpOffer : offerSdp
	}
	sendMessage(message);
}*/

function onError(error) {
	console.error(error);
}

function onIceCandidate1(candidate) {
	console.log('Local candidate' + JSON.stringify(candidate));

	var message = {
			id : 'onIceCandidate',
			num: '1',
			candidate : candidate
	};
	sendMessage(message);
}

function onIceCandidate2(candidate) {
	console.log('Local candidate' + JSON.stringify(candidate));

	var message = {
			id : 'onIceCandidate',
			num: '2',
			candidate : candidate
	};
	sendMessage(message);
}

function onIceCandidate3(candidate) {
	console.log('Local candidate' + JSON.stringify(candidate));

	var message = {
			id : 'onIceCandidate',
			num: '3',
			candidate : candidate
	};
	sendMessage(message);
}

function onIceCandidate4(candidate) {
	console.log('Local candidate' + JSON.stringify(candidate));

	var message = {
			id : 'onIceCandidate',
			num: '4',
			candidate : candidate
	};
	sendMessage(message);
}

/*function onIceCandidate5(candidate) {
	console.log('Local candidate' + JSON.stringify(candidate));

	var message = {
			id : 'onIceCandidate',
			num: '5',
			candidate : candidate
	};
	sendMessage(message);
}

function onIceCandidate6(candidate) {
	console.log('Local candidate' + JSON.stringify(candidate));

	var message = {
			id : 'onIceCandidate',
			num: '6',
			candidate : candidate
	};
	sendMessage(message);
}*/

function startResponse(message) {
	setState(IN_CALL);
	console.log('SDP answer received from server. Processing ...');
	switch (message.num) {
	case '1':
		webRtcPeer1.processAnswer(message.sdpAnswer, function(error) {
			if (error)
				return console.error(error);
		});
		break;
	case '2':
		webRtcPeer2.processAnswer(message.sdpAnswer, function(error) {
			if (error)
				return console.error(error);
		});
		break;
	case '3':
		webRtcPeer3.processAnswer(message.sdpAnswer, function(error) {
			if (error)
				return console.error(error);
		});
		break;
	case '4':
		webRtcPeer4.processAnswer(message.sdpAnswer, function(error) {
			if (error)
				return console.error(error);
		});
		break;
	/*case '5':
		webRtcPeer5.processAnswer(message.sdpAnswer, function(error) {
			if (error)
				return console.error(error);
		});
		break;
	case '6':
		webRtcPeer6.processAnswer(message.sdpAnswer, function(error) {
			if (error)
				return console.error(error);
		});
		break;*/
	}
}

function stop() {
	var stopMessageId = 'stop';
	console.log('Stopping video while in ' + state + '...');
	setState(NO_CALL);
	if (webRtcPeer1) {
		webRtcPeer1.dispose();
		webRtcPeer1 = null;

		var message = {
				id : stopMessageId,
				num : '1'
		}
		sendMessage(message);
	}
	if (webRtcPeer2) {
		webRtcPeer2.dispose();
		webRtcPeer2 = null;

		var message = {
				id : stopMessageId,
				num : '2'
		}
		sendMessage(message);
	}
	if (webRtcPeer3) {
		webRtcPeer3.dispose();
		webRtcPeer3 = null;

		var message = {
				id : stopMessageId,
				num : '3'
		}
		sendMessage(message);
	}
	if (webRtcPeer4) {
		webRtcPeer4.dispose();
		webRtcPeer4 = null;

		var message = {
				id : stopMessageId,
				num : '4'
		}
		sendMessage(message);
	}
	/*if (webRtcPeer5) {
		webRtcPeer5.dispose();
		webRtcPeer5 = null;

		var message = {
				id : stopMessageId,
				num : '5'
		}
		sendMessage(message);
	}
	if (webRtcPeer6) {
		webRtcPeer6.dispose();
		webRtcPeer6 = null;

		var message = {
				id : stopMessageId,
				num : '6'
		}
		sendMessage(message);
	}*/
	hideSpinner(video1,video2,video3,video4/*,video5,video6*/);
}

function playEnd() {
	setState(NO_CALL);
	hideSpinner(video1,video2,video3,video4/*,video5,video6*/);
}

function sendMessage(message) {
	var jsonMessage = JSON.stringify(message);
	console.log('Senging message: ' + jsonMessage);
	ws.send(jsonMessage);
}

function showSpinner() {
	for (var i = 0; i < arguments.length; i++) {
		arguments[i].poster = './img/transparent-1px.png';
		arguments[i].style.background = "center transparent url('./img/spinner.gif') no-repeat";
	}
}

function hideSpinner() {
	for (var i = 0; i < arguments.length; i++) {
		arguments[i].src = '';
		arguments[i].poster = './img/webrtc.png';
		arguments[i].style.background = '';
	}
}
/**
 * Lightbox utility (to display media pipeline image in a modal dialog)
 */
$(document).delegate('*[data-toggle="lightbox"]', 'click', function(event) {
	event.preventDefault();
	$(this).ekkoLightbox();
});
