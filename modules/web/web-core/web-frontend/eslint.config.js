import { defineConfig, globalIgnores } from "eslint/config"
import globals from "globals"
import typescriptEslint from "typescript-eslint"
import vue from "eslint-plugin-vue"
import js from "@eslint/js"
import { FlatCompat } from "@eslint/eslintrc"
import { createRequire } from "node:module"

const require = createRequire(import.meta.url)

const compat = new FlatCompat({
	baseDirectory: import.meta.dirname,
	recommendedConfig: js.configs.recommended,
	allConfig: js.configs.all,
})

export default defineConfig([
	{
		languageOptions: {
			parserOptions: {
				extraFileExtensions: [".js", ".ts", ".vue"],
				parser: require.resolve("@typescript-eslint/parser"),
			},

			globals: {
				...globals.browser,
				...globals.node,
				...vue.environments["setup-compiler-macros"]["setup-compiler-macros"],
				ga: "readonly",
				cordova: "readonly",
				__statics: "readonly",
				process: "readonly",
				Capacitor: "readonly",
				chrome: "readonly",
			},
		},

		ignores: ["/dist", "/node_modules", ".eslintrc.js", "/build"],

		extends: compat.extends(
			"plugin:@typescript-eslint/recommended",
			"plugin:vue/vue3-essential",
			"plugin:vue/vue3-strongly-recommended",
			"plugin:vue/vue3-recommended",
			"prettier"
		),

		plugins: {
			typescriptEslint,
			vue,
		},

		rules: {
			"prefer-promise-reject-errors": "off",

			quotes: [
				"warn",
				"double",
				{
					avoidEscape: true,
				},
			],

			"@typescript-eslint/explicit-function-return-type": "off",
			"@typescript-eslint/no-var-requires": "off",
			"no-unused-vars": "off",
			"no-debugger": process.env.NODE_ENV === "production" ? "error" : "off",
		},
	},
	globalIgnores(["dist", "node_modules", "**/.eslintrc.js", "build"]),
])
