package com.dellemc.pravega.app;
public class Constants {
    protected static final String DEFAULT_SCOPE = "project55"; 
    protected static final String DEFAULT_STREAM_NAME = "eventapp"; 
    protected static final String DEFAULT_CONTROLLER_URI = "nautilus-pravega-controller.nautilus-pravega:9090";
    protected static final String DEFAULT_ROUTING_KEY = ""; 
    protected static final int NO_OF_SEGMENTS = 1;
    protected static final String DEFAULT_MESSAGE = "{\"kind\":\"Event\",\"apiVersion\":\"audit.k8s.io/v1\",\"level\":\"Metadata\",\"auditID\":\"c6e7742b-dee4-4879-8d1f-b876fff54d6a\",\"stage\":\"ResponseComplete\",\"requestURI\":\"/api/v1/namespaces/cert-manager/secrets/cert-manager-cainjector-token-t26jc\",\"verb\":\"get\",\"user\":{\"username\":\"system:apiserver\",\"uid\":\"cf4088dc-1ebe-4bef-a72a-448fb1e9d232\",\"groups\":[\"system:masters\"]},\"sourceIPs\":[\"127.0.0.1\"],\"userAgent\":\"kube-apiserver/v1.13.5 (linux/amd64) kubernetes/2166946\",\"objectRef\":{\"resource\":\"secrets\",\"namespace\":\"cert-manager\",\"name\":\"cert-manager-cainjector-token-t26jc\",\"apiVersion\":\"v1\"},\"responseStatus\":{\"metadata\":{},\"code\":200},\"requestReceivedTimestamp\":\"2019-09-15T13:57:40.904647Z\",\"stageTimestamp\":\"2019-09-15T13:57:40.905910Z\",\"annotations\":{\"authorization.k8s.io/decision\":\"allow\",\"authorization.k8s.io/reason\":\"\"}}";
}

