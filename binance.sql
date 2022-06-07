--
-- PostgreSQL database dump
--

-- Dumped from database version 12.10 (Ubuntu 12.10-0ubuntu0.20.04.1)
-- Dumped by pg_dump version 12.10 (Ubuntu 12.10-0ubuntu0.20.04.1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: compra; Type: TABLE; Schema: public; Owner: juan
--

CREATE TABLE public.compra (
    id bigint NOT NULL,
    fechacompra timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    preciocompra numeric NOT NULL,
    fechaventa timestamp without time zone,
    precioventa numeric,
    ganancia numeric DEFAULT 0 NOT NULL,
    totalinvertido numeric NOT NULL,
    totalinvertidoganancia numeric DEFAULT 0 NOT NULL,
    totalinvertidogananciacrypto numeric DEFAULT 0 NOT NULL,
    valorventaavisomail numeric DEFAULT 0 NOT NULL,
    porcentajeventaavisomail numeric DEFAULT 0 NOT NULL,
    enviarmail boolean,
    siglacrypto text NOT NULL
);


ALTER TABLE public.compra OWNER TO juan;

--
-- Name: compra_id_seq; Type: SEQUENCE; Schema: public; Owner: juan
--

CREATE SEQUENCE public.compra_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.compra_id_seq OWNER TO juan;

--
-- Name: compra_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: juan
--

ALTER SEQUENCE public.compra_id_seq OWNED BY public.compra.id;


--
-- Name: parametro; Type: TABLE; Schema: public; Owner: juan
--

CREATE TABLE public.parametro (
    id bigint NOT NULL,
    codparam text NOT NULL,
    valor text NOT NULL,
    enviarmail boolean DEFAULT false
);


ALTER TABLE public.parametro OWNER TO juan;

--
-- Name: parametro_id_seq; Type: SEQUENCE; Schema: public; Owner: juan
--

CREATE SEQUENCE public.parametro_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.parametro_id_seq OWNER TO juan;

--
-- Name: parametro_id_seq1; Type: SEQUENCE; Schema: public; Owner: juan
--

CREATE SEQUENCE public.parametro_id_seq1
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.parametro_id_seq1 OWNER TO juan;

--
-- Name: parametro_id_seq1; Type: SEQUENCE OWNED BY; Schema: public; Owner: juan
--

ALTER SEQUENCE public.parametro_id_seq1 OWNED BY public.parametro.id;


--
-- Name: trade_id_seq; Type: SEQUENCE; Schema: public; Owner: juan
--

CREATE SEQUENCE public.trade_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.trade_id_seq OWNER TO juan;

--
-- Name: trade; Type: TABLE; Schema: public; Owner: juan
--

CREATE TABLE public.trade (
    id bigint DEFAULT nextval('public.trade_id_seq'::regclass) NOT NULL,
    opentime bigint NOT NULL,
    closetime bigint,
    entryprice character varying(300) NOT NULL,
    closeprice character varying(300),
    amount character varying(300),
    total character varying(300),
    currency character varying(300) NOT NULL
);


ALTER TABLE public.trade OWNER TO juan;

--
-- Name: trading; Type: TABLE; Schema: public; Owner: juan
--

CREATE TABLE public.trading (
    id bigint NOT NULL,
    fechacompra timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    preciocompra numeric(10,2) DEFAULT 0,
    fechaventa timestamp without time zone,
    precioventa numeric(10,2),
    ganancia numeric(10,2) DEFAULT 0,
    totalinvertido numeric(10,2) NOT NULL,
    totalinvertidoganancia numeric(10,2) DEFAULT 0,
    totalinvertidogananciacrypto numeric(10,2) DEFAULT 0,
    siglacrypto text NOT NULL
);


ALTER TABLE public.trading OWNER TO juan;

--
-- Name: trading_id; Type: SEQUENCE; Schema: public; Owner: juan
--

CREATE SEQUENCE public.trading_id
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.trading_id OWNER TO juan;

--
-- Name: trading_id_seq; Type: SEQUENCE; Schema: public; Owner: juan
--

CREATE SEQUENCE public.trading_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.trading_id_seq OWNER TO juan;

--
-- Name: trading_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: juan
--

ALTER SEQUENCE public.trading_id_seq OWNED BY public.trading.id;


--
-- Name: tradingdiario; Type: TABLE; Schema: public; Owner: juan
--

CREATE TABLE public.tradingdiario (
    id bigint NOT NULL,
    fechacompra timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    preciocompra numeric(10,2) DEFAULT 0,
    fechaventa timestamp without time zone,
    precioventa numeric(10,2),
    ganancia numeric(10,2) DEFAULT 0,
    totalinvertido numeric(10,2) NOT NULL,
    totalinvertidoganancia numeric(10,2) DEFAULT 0,
    totalinvertidogananciacrypto numeric(10,2) DEFAULT 0,
    siglacrypto text NOT NULL
);


ALTER TABLE public.tradingdiario OWNER TO juan;

--
-- Name: tradingdiario_id_seq; Type: SEQUENCE; Schema: public; Owner: juan
--

CREATE SEQUENCE public.tradingdiario_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.tradingdiario_id_seq OWNER TO juan;

--
-- Name: tradingdiario_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: juan
--

ALTER SEQUENCE public.tradingdiario_id_seq OWNED BY public.tradingdiario.id;


--
-- Name: compra id; Type: DEFAULT; Schema: public; Owner: juan
--

ALTER TABLE ONLY public.compra ALTER COLUMN id SET DEFAULT nextval('public.compra_id_seq'::regclass);


--
-- Name: parametro id; Type: DEFAULT; Schema: public; Owner: juan
--

ALTER TABLE ONLY public.parametro ALTER COLUMN id SET DEFAULT nextval('public.parametro_id_seq1'::regclass);


--
-- Name: trading id; Type: DEFAULT; Schema: public; Owner: juan
--

ALTER TABLE ONLY public.trading ALTER COLUMN id SET DEFAULT nextval('public.trading_id_seq'::regclass);


--
-- Name: tradingdiario id; Type: DEFAULT; Schema: public; Owner: juan
--

ALTER TABLE ONLY public.tradingdiario ALTER COLUMN id SET DEFAULT nextval('public.tradingdiario_id_seq'::regclass);


--
-- Data for Name: compra; Type: TABLE DATA; Schema: public; Owner: juan
--

COPY public.compra (id, fechacompra, preciocompra, fechaventa, precioventa, ganancia, totalinvertido, totalinvertidoganancia, totalinvertidogananciacrypto, valorventaavisomail, porcentajeventaavisomail, enviarmail, siglacrypto) FROM stdin;
\.


--
-- Data for Name: parametro; Type: TABLE DATA; Schema: public; Owner: juan
--

COPY public.parametro (id, codparam, valor, enviarmail) FROM stdin;
2	valorsubeavisomail	2800 	t
1	valorbajaavisomail	2650	f
\.


--
-- Data for Name: trade; Type: TABLE DATA; Schema: public; Owner: juan
--

COPY public.trade (id, opentime, closetime, entryprice, closeprice, amount, total, currency) FROM stdin;
8	1651759050000	1652230776000	39065.88	31036.16	0.00083	32,4247	BTCUSDT
9	1651759528000	1652285211000	2880.56	\t2259.40792793	0.0111	\t31.9742162259.40792793	ETHUSDT
11	1651730113000	1652289739226	95.2	62.8998334	0.347	33.0344	DASHUSDT
10	1651729809000	1652289741911	0.8674	0.5615985	38.1	33.04794	ADAUSDT
12	1651733584000	1652289744863	1.16	0.7179981	28.5	33.06	MATICUSDT
13	1651759117000	1652289746988	89.86	53.0112882	0.35	31.451	SOLUSDT
14	1652323518000	1652324782000	50.76	47.73	0.49	24.8724	SOLUSDT
15	1652329393379	1652398577374	1896.1061061	1970.6373899	0.01289	24.4353090	ETHUSDT
16	1652484606774	1652547078476	29528.8488488	29481.2492402	0.00484	142.7767088	BTCUSDT
17	1652666099999	\N	7.6676677	\N	18.62136	142.7824000	LINKUSDT
18	1652666158506	\N	2094.4496747	\N	0.05841	122.3381250	ETHUSDT
\.


--
-- Data for Name: trading; Type: TABLE DATA; Schema: public; Owner: juan
--

COPY public.trading (id, fechacompra, preciocompra, fechaventa, precioventa, ganancia, totalinvertido, totalinvertidoganancia, totalinvertidogananciacrypto, siglacrypto) FROM stdin;
62	2022-03-04 09:06:20.688313	2746.74	2022-03-04 09:25:59.818856	2751.74	-5.44	752.09	746.65	0.27	ETH
22	2022-03-04 04:18:34.201805	2723.36	2022-03-04 04:57:40.480802	2728.36	2.27	751.92	754.19	0.28	ETH
21	2022-03-04 04:16:33.684382	2725.77	2022-03-04 04:57:40.483046	2730.77	1.61	751.92	753.53	0.28	ETH
25	2022-03-04 04:55:02.333107	2725.99	2022-03-04 04:57:40.484747	2730.99	1.55	754.81	756.36	0.28	ETH
59	2022-03-04 07:50:46.408087	2744.22	2022-03-04 08:49:43.50451	2749.22	1.54	750.11	751.65	0.27	ETH
65	2022-03-04 09:21:44.645883	2736.72	2022-03-04 09:40:29.017461	2741.72	1.37	750.04	751.41	0.27	ETH
35	2022-03-04 05:34:31.319153	2710.99	2022-03-04 05:37:35.611936	2715.99	1.44	749.19	750.63	0.28	ETH
84	2022-03-04 10:46:49.246622	2725.50	\N	2730.50	-1.09	748.22	747.13	0.27	ETH
18	2022-03-04 03:53:07.163724	2725.52	2022-03-04 04:09:56.376274	2730.52	1.50	750.00	751.50	0.28	ETH
26	2022-03-04 04:59:23.01021	2731.00	2022-03-04 05:01:44.733559	2736.00	1.38	754.69	756.07	0.28	ETH
33	2022-03-04 05:30:25.879155	2726.75	2022-03-04 05:31:32.708704	2731.75	-5.16	750.20	745.04	0.28	ETH
30	2022-03-04 05:15:21.620791	2746.34	2022-03-04 05:30:23.881636	2751.34	-5.22	755.35	750.13	0.28	ETH
29	2022-03-04 05:12:17.497458	2745.80	2022-03-04 05:30:23.883486	2750.80	-5.07	755.35	750.28	0.28	ETH
43	2022-03-04 06:03:55.630097	2742.49	2022-03-04 06:05:19.882237	2747.49	1.50	752.20	753.70	0.27	ETH
42	2022-03-04 06:01:51.216975	2742.88	2022-03-04 06:03:00.158549	2747.88	1.39	751.85	753.24	0.27	ETH
28	2022-03-04 05:09:06.81826	2739.09	2022-03-04 05:09:52.958908	2744.09	1.51	754.97	756.48	0.28	ETH
57	2022-03-04 07:46:13.505718	2744.87	2022-03-04 08:49:45.611501	2749.87	1.37	750.11	751.48	0.27	ETH
23	2022-03-04 04:20:42.60564	2718.39	2022-03-04 04:25:29.461742	2723.39	1.42	751.92	753.34	0.28	ETH
45	2022-03-04 06:09:18.438693	2742.88	2022-03-04 06:17:19.362535	2747.88	-5.54	752.57	747.03	0.27	ETH
34	2022-03-04 05:32:29.4456	2706.39	2022-03-04 05:32:50.008705	2711.39	1.38	748.85	750.23	0.28	ETH
20	2022-03-04 04:13:03.091686	2718.01	2022-03-04 04:14:42.896017	2723.01	1.38	751.46	752.84	0.28	ETH
17	2022-03-04 03:48:21.924606	2722.03	2022-03-04 04:08:32.099554	2727.03	1.42	750.00	751.42	0.28	ETH
58	2022-03-04 07:48:43.050112	2744.86	2022-03-04 08:49:45.613389	2749.86	1.37	750.11	751.48	0.27	ETH
54	2022-03-04 06:37:52.023713	2737.66	2022-03-04 07:33:42.429547	2742.66	1.52	749.46	750.98	0.27	ETH
16	2022-03-04 03:46:12.055371	2722.60	2022-03-04 04:08:34.269186	2727.60	1.38	750.00	751.38	0.28	ETH
48	2022-03-04 06:17:17.369539	2725.17	2022-03-04 06:21:02.451364	2730.17	1.37	747.28	748.65	0.27	ETH
15	2022-03-04 03:44:08.525875	2722.78	2022-03-04 04:08:36.305163	2727.78	1.55	750.00	751.55	0.28	ETH
44	2022-03-04 06:06:11.820275	2744.64	2022-03-04 06:16:27.270069	2749.64	-5.29	752.57	747.28	0.27	ETH
40	2022-03-04 05:57:26.502428	2741.46	2022-03-04 06:01:01.080792	2746.46	1.41	751.10	752.51	0.27	ETH
41	2022-03-04 05:59:28.500089	2740.83	2022-03-04 06:01:01.092213	2745.83	1.59	751.10	752.69	0.27	ETH
19	2022-03-04 04:10:58.743341	2728.72	2022-03-04 05:00:17.902854	2733.72	1.54	751.46	753.00	0.28	ETH
50	2022-03-04 06:28:24.309978	2736.96	2022-03-04 07:33:42.431098	2741.96	1.71	747.49	749.20	0.27	ETH
32	2022-03-04 05:19:25.733766	2742.48	2022-03-04 05:30:40.703238	2747.48	-5.29	755.35	750.06	0.28	ETH
31	2022-03-04 05:17:25.528784	2742.42	2022-03-04 05:30:40.704966	2747.42	-5.27	755.35	750.08	0.28	ETH
52	2022-03-04 06:33:30.096332	2737.98	2022-03-04 07:33:42.433048	2742.98	1.43	747.95	749.38	0.27	ETH
51	2022-03-04 06:31:29.964228	2738.47	2022-03-04 07:33:48.887507	2743.47	1.53	747.95	749.48	0.27	ETH
27	2022-03-04 05:04:08.611396	2733.30	2022-03-04 05:07:53.619206	2738.30	1.43	754.61	756.04	0.28	ETH
74	2022-03-04 09:52:29.376285	2728.12	2022-03-04 10:30:06.261512	2733.12	1.74	750.60	752.34	0.28	ETH
38	2022-03-04 05:50:30.45924	2736.07	2022-03-04 05:56:24.763315	2741.07	1.67	750.32	751.99	0.27	ETH
24	2022-03-04 04:30:34.923695	2721.50	2022-03-04 04:53:25.052101	2726.50	1.47	753.34	754.81	0.28	ETH
55	2022-03-04 07:34:26.217761	2740.53	2022-03-04 07:36:38.8767	2745.53	1.40	749.76	751.16	0.27	ETH
36	2022-03-04 05:40:43.004718	2730.59	2022-03-04 05:41:56.314262	2735.59	1.38	749.55	750.93	0.27	ETH
46	2022-03-04 06:11:20.294935	2739.35	2022-03-04 06:17:29.667977	2744.35	-5.63	752.57	746.94	0.27	ETH
64	2022-03-04 09:11:54.191649	2734.00	2022-03-04 09:17:22.47307	2739.00	1.45	749.32	750.77	0.27	ETH
53	2022-03-04 06:35:32.245352	2734.48	2022-03-04 06:37:13.12737	2739.48	1.51	747.95	749.46	0.27	ETH
67	2022-03-04 09:26:00.160526	2726.76	2022-03-04 09:36:40.362141	2731.76	1.45	746.65	748.10	0.27	ETH
37	2022-03-04 05:48:10.793613	2732.54	2022-03-04 05:48:27.479389	2737.54	1.67	749.90	751.57	0.27	ETH
39	2022-03-04 05:52:31.971929	2734.61	2022-03-04 05:55:07.543794	2739.61	1.47	750.32	751.79	0.27	ETH
47	2022-03-04 06:14:28.464775	2736.90	2022-03-04 06:17:33.722384	2741.90	-5.22	752.57	747.35	0.27	ETH
56	2022-03-04 07:44:13.184139	2745.56	2022-03-04 08:53:02.166833	2750.56	1.69	750.11	751.80	0.27	ETH
76	2022-03-04 10:01:00.150532	2728.72	2022-03-04 10:30:06.263647	2733.72	1.57	746.96	748.53	0.27	ETH
49	2022-03-04 06:25:54.831968	2736.56	2022-03-04 06:30:13.902083	2741.56	1.38	747.49	748.87	0.27	ETH
69	2022-03-04 09:40:07.068304	2738.57	2022-03-04 09:40:39.551462	2743.57	1.43	748.29	749.72	0.27	ETH
60	2022-03-04 08:58:04.818626	2751.58	2022-03-04 09:10:02.285365	2756.58	-5.06	751.60	746.54	0.27	ETH
68	2022-03-04 09:33:06.819526	2722.69	2022-03-04 09:34:26.629187	2727.69	1.45	747.03	748.48	0.27	ETH
79	2022-03-04 10:32:52.259769	2745.48	2022-03-04 10:35:23.742122	2750.48	-5.45	750.59	745.14	0.27	ETH
71	2022-03-04 09:46:15.611522	2743.45	2022-03-04 09:56:26.073608	2748.45	-5.02	750.60	745.58	0.27	ETH
75	2022-03-04 09:56:28.209855	2724.85	2022-03-04 09:59:52.502403	2729.85	1.38	745.58	746.96	0.27	ETH
61	2022-03-04 09:03:15.903653	2742.20	2022-03-04 09:05:02.16071	2747.20	1.46	751.60	753.06	0.27	ETH
63	2022-03-04 09:08:22.097985	2741.58	2022-03-04 09:32:57.819562	2746.58	-5.06	752.09	747.03	0.27	ETH
72	2022-03-04 09:48:16.766447	2738.34	2022-03-04 10:14:48.846751	2743.34	-5.18	750.60	745.42	0.27	ETH
70	2022-03-04 09:42:33.594989	2738.83	2022-03-04 09:45:50.54606	2743.83	1.41	750.25	751.66	0.27	ETH
80	2022-03-04 10:34:52.566268	2737.06	2022-03-04 10:36:04.264316	2742.06	-5.63	750.59	744.96	0.27	ETH
66	2022-03-04 09:23:44.739279	2736.01	2022-03-04 09:40:26.836909	2741.01	1.55	750.04	751.59	0.27	ETH
78	2022-03-04 10:16:52.464615	2723.03	2022-03-04 10:26:17.649533	2728.03	1.41	746.88	748.29	0.27	ETH
77	2022-03-04 10:14:51.37623	2718.88	2022-03-04 10:15:14.143037	2723.88	1.46	745.42	746.88	0.27	ETH
85	2022-03-04 10:50:00.165806	2723.76	\N	2728.76	-0.61	748.22	747.61	0.27	ETH
73	2022-03-04 09:50:18.470509	2731.50	2022-03-04 10:30:10.299438	2736.50	2.60	750.60	753.20	0.27	ETH
81	2022-03-04 10:38:51.199547	2725.52	2022-03-04 10:40:22.091075	2730.52	1.61	747.82	749.43	0.27	ETH
82	2022-03-04 10:41:13.440337	2727.87	\N	2732.87	-1.74	748.22	746.48	0.27	ETH
83	2022-03-04 10:44:47.935819	2727.53	\N	2732.53	-1.65	748.22	746.57	0.27	ETH
\.


--
-- Data for Name: tradingdiario; Type: TABLE DATA; Schema: public; Owner: juan
--

COPY public.tradingdiario (id, fechacompra, preciocompra, fechaventa, precioventa, ganancia, totalinvertido, totalinvertidoganancia, totalinvertidogananciacrypto, siglacrypto) FROM stdin;
2	2022-03-04 19:30:57.92391	2573.41	\N	2650.00	-0.17	750.00	749.83	0.29	ETH
1	\N	2550.00	\N	2600.00	0.00	750.00	0.00	0.00	ETH
\.


--
-- Name: compra_id_seq; Type: SEQUENCE SET; Schema: public; Owner: juan
--

SELECT pg_catalog.setval('public.compra_id_seq', 2, true);


--
-- Name: parametro_id_seq; Type: SEQUENCE SET; Schema: public; Owner: juan
--

SELECT pg_catalog.setval('public.parametro_id_seq', 1, false);


--
-- Name: parametro_id_seq1; Type: SEQUENCE SET; Schema: public; Owner: juan
--

SELECT pg_catalog.setval('public.parametro_id_seq1', 2, true);


--
-- Name: trade_id_seq; Type: SEQUENCE SET; Schema: public; Owner: juan
--

SELECT pg_catalog.setval('public.trade_id_seq', 18, true);


--
-- Name: trading_id; Type: SEQUENCE SET; Schema: public; Owner: juan
--

SELECT pg_catalog.setval('public.trading_id', 1, false);


--
-- Name: trading_id_seq; Type: SEQUENCE SET; Schema: public; Owner: juan
--

SELECT pg_catalog.setval('public.trading_id_seq', 85, true);


--
-- Name: tradingdiario_id_seq; Type: SEQUENCE SET; Schema: public; Owner: juan
--

SELECT pg_catalog.setval('public.tradingdiario_id_seq', 2, true);


--
-- PostgreSQL database dump complete
--

