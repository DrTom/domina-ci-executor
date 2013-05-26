(ns domina-ci.executor.certificate
  (:import
    [java.security KeyPairGenerator Security SecureRandom]
    [org.bouncycastle.jce X509Principal]
    [org.bouncycastle.jce.provider BouncyCastleProvider]
    [org.bouncycastle.x509 X509V3CertificateGenerator]
    [java.util Date]
    ))

(defn create-certificate []

  (Security/addProvider (BouncyCastleProvider.))

  (let [generator (.getInstace KeyPairGenerator "RSA")
        _  (.initialize generator 2048 (SecureRandom.))
        key-pair (.generateKeyPair generator)
        cert_gen (X509V3CertificateGenerator.) 
        year_in_millis (* 1000 60 60 24 365)
        ]

    (.setSerialNumber cert_gen  (rand-int 1000000000))
    (.setIssuerDN cert_gen (X509Principal. "CN=cn, O=o, L=L, ST=il, C= c"))
    (.setSubjectDN cert_gen (X509Principal. "CN=cn, O=o, L=L, ST=il, C= c"))
    (.setNotBefore cert_gen (Date. (- (System/currentTimeMillis) year_in_millis )))
    (.setNotAfter cert_gen (Date. (+ (System/currentTimeMillis) (* year_in_millis * 100))))
    (.setPublicKey cert_gen (.getPublic key-pair))
    (.setSignatureAlgorithm cert_gen "SHA256WithRSAEncryption")

    ;{:certificate (.generateX509Certificate cert_gen (.getPrivate key-pair))
    ;:key-pair key-pair}
    
    ))
