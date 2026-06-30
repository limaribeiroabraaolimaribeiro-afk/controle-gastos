package com.controlpro.banklistener.data.model

data class AllowedApp(
    val packageName: String,
    val displayName: String,
    val isEnabled: Boolean = true
)

object DefaultBankApps {
    val list = listOf(
        AllowedApp("com.nu.production", "Nubank"),
        AllowedApp("com.picpay", "PicPay"),
        AllowedApp("com.mercadopago.wallet", "Mercado Pago"),
        AllowedApp("br.com.gabba.Caixa", "Caixa Econômica"),
        AllowedApp("br.com.intermedium", "Banco Inter"),
        AllowedApp("com.itau", "Itaú"),
        AllowedApp("com.bradesco", "Bradesco"),
        AllowedApp("br.com.santander.benio", "Santander"),
        AllowedApp("br.com.bb.android", "Banco do Brasil"),
        AllowedApp("com.c6bank.app", "C6 Bank"),
        AllowedApp("br.com.infinitepay.mobile", "InfinitePay"),
        AllowedApp("br.com.stone.bank", "Stone"),
        AllowedApp("br.com.uol.ps.myaccount", "PagBank"),
        AllowedApp("br.com.neonapp", "Neon"),
        AllowedApp("com.will.bank", "Will Bank"),
        AllowedApp("br.com.original.bancodobrasil", "Banco Original"),
        AllowedApp("br.com.sicoob.sicoobnet.mobile", "Sicoob"),
        AllowedApp("br.com.banrisul.appbanrisul", "Banrisul"),
        AllowedApp("br.com.banese.mobile", "Banese"),
        AllowedApp("br.com.sicredi.mobile", "Sicredi")
    )
}
